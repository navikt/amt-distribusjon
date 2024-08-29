package no.nav.amt.distribusjon.journalforing

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.HendelseDto
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.journalforing.model.HendelseMedJournalforingstatus
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.MockResponseHandler
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.utils.data.Persondata
import no.nav.amt.distribusjon.utils.produceStringString
import no.nav.amt.lib.testing.AsyncUtils
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDateTime

class JournalforingServiceTest {
    @Test
    fun `handleHendelse - InnbyggerGodkjennUtkast - journalforer hovedvedtak`() = integrationTest { app, _ ->
        val hendelseDto = Hendelsesdata.hendelseDto(HendelseTypeData.innbyggerGodkjennUtkast())

        produce(hendelseDto)

        AsyncUtils.eventually {
            app.journalforingstatusRepository.get(hendelseDto.id)!!.journalpostId shouldNotBe null
        }
    }

    @Test
    fun `handleHendelse - InnbyggerGodkjennUtkast, er allerede journalfort, skal ikke sende brev - ignorerer hendelse`() = integrationTest {
            app,
            _,
        ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast())

        val journalpostId = "12345"

        app.hendelseRepository.insert(hendelse)
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelse.id, journalpostId, null))

        app.journalforingService.handleHendelse(hendelse)

        val status = app.journalforingstatusRepository.get(hendelse.id)
        status!!.journalpostId shouldBe journalpostId
    }

    @Test
    fun `handleHendelse - NavGodkjennUtkast, er journalfort, ikke sendt brev - sender brev`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.navGodkjennUtkast(), distribusjonskanal = Distribusjonskanal.PRINT)

        val journalpostId = "12345"

        app.hendelseRepository.insert(hendelse)
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelse.id, journalpostId, null))

        app.journalforingService.handleHendelse(hendelse)

        val status = app.journalforingstatusRepository.get(hendelse.id)
        status!!.journalpostId shouldBe journalpostId
        status.bestillingsId shouldNotBe null
    }

    @Test
    fun `handleHendelse - NavGodkjennUtkast, manuell oppfolging - sender brev`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(
            HendelseTypeData.navGodkjennUtkast(),
            distribusjonskanal = Distribusjonskanal.SDP,
            manuellOppfolging = true,
        )

        app.hendelseRepository.insert(hendelse)

        app.journalforingService.handleHendelse(hendelse)

        val status = app.journalforingstatusRepository.get(hendelse.id)
        status!!.journalpostId shouldNotBe null
        status.bestillingsId shouldNotBe null
    }

    @Test
    fun `handleHendelse - AvsluttDeltakelse, er allerede journalfort, skal ikke sende brev - ignorerer hendelse`() = integrationTest {
            app,
            _,
        ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.avsluttDeltakelse())

        val journalpostId = "12345"

        app.hendelseRepository.insert(hendelse)
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelse.id, journalpostId, null))

        app.journalforingService.handleHendelse(hendelse)

        val status = app.journalforingstatusRepository.get(hendelse.id)
        status!!.journalpostId shouldNotBe null
    }

    @Test
    fun `handleHendelse - InnbyggerGodkjennUtkast, har ikke aktiv oppfolgingsperiode - feiler`() = integrationTest { app, _ ->
        val navBruker = Persondata.lagNavBruker(
            oppfolgingsperioder = listOf(
                Persondata.lagOppfolgingsperiode(
                    startdato = LocalDateTime.now().minusYears(2),
                    sluttdato = LocalDateTime.now().minusMonths(4),
                ),
            ),
        )

        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.innbyggerGodkjennUtkast())

        MockResponseHandler.addNavBrukerResponse(hendelse.deltaker.personident, navBruker)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                app.journalforingService.handleHendelse(hendelse.toModel(Distribusjonskanal.DITT_NAV, false))
            }
        }
    }

    @Test
    fun `handleHendelse - EndreSluttarsak - journalforer ikke`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.endreSluttarsak())

        app.journalforingService.handleHendelse(hendelse)
        app.journalforingstatusRepository.get(hendelse.id) shouldBe null
    }

    @Test
    fun `journalforOgDistribuerEndringsvedtak - deltakelsesmengde og forleng - journalforer endringsvedtak`() = integrationTest { app, _ ->
        val deltaker = Hendelsesdata.deltaker()

        val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
            HendelseTypeData.endreDeltakelsesmengde(),
            deltaker = deltaker,
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        val journalforingstatusDeltakelsesmengde = Journalforingstatus(hendelseDeltakelsesmengde.id, null, null)
        app.journalforingstatusRepository.upsert(journalforingstatusDeltakelsesmengde)
        val hendelseForleng = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(),
            deltaker = deltaker,
            ansvarlig = hendelseDeltakelsesmengde.ansvarlig,
            opprettet = LocalDateTime.now(),
        )
        val journalforingstatusForleng = Journalforingstatus(hendelseForleng.id, null, null)
        app.journalforingstatusRepository.upsert(journalforingstatusForleng)

        app.journalforingService.journalforOgDistribuerEndringsvedtak(
            listOf(
                HendelseMedJournalforingstatus(hendelseForleng, journalforingstatusForleng),
                HendelseMedJournalforingstatus(hendelseDeltakelsesmengde, journalforingstatusDeltakelsesmengde),
            ),
        )

        val journalpostDeltakelsesmengde = app.journalforingstatusRepository.get(hendelseDeltakelsesmengde.id)!!
        val journalpostForleng = app.journalforingstatusRepository.get(hendelseForleng.id)!!

        journalpostDeltakelsesmengde.journalpostId shouldNotBe null
        journalpostDeltakelsesmengde.journalpostId shouldBe journalpostForleng.journalpostId
    }

    @Test
    fun `journalforOgDistribuerEndringsvedtak - to endringer, en allerede journalfort - journalforer 1, distribuerer 2`() =
        integrationTest {
                app,
                _,
            ->
            val deltaker = Hendelsesdata.deltaker()

            val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
                HendelseTypeData.endreDeltakelsesmengde(),
                deltaker = deltaker,
                opprettet = LocalDateTime.now().minusMinutes(20),
                distribusjonskanal = Distribusjonskanal.PRINT,
            )
            val journalforingstatusDeltakelsesmengde = Journalforingstatus(hendelseDeltakelsesmengde.id, "99887", null)
            app.journalforingstatusRepository.upsert(journalforingstatusDeltakelsesmengde)

            val hendelseForleng = Hendelsesdata.hendelse(
                HendelseTypeData.forlengDeltakelse(),
                deltaker = deltaker,
                ansvarlig = hendelseDeltakelsesmengde.ansvarlig,
                opprettet = LocalDateTime.now(),
                distribusjonskanal = Distribusjonskanal.PRINT,
            )
            val journalforingstatusForleng = Journalforingstatus(hendelseForleng.id, null, null)
            app.journalforingstatusRepository.upsert(journalforingstatusForleng)

            app.journalforingService.journalforOgDistribuerEndringsvedtak(
                listOf(
                    HendelseMedJournalforingstatus(hendelseForleng, journalforingstatusForleng),
                    HendelseMedJournalforingstatus(hendelseDeltakelsesmengde, journalforingstatusDeltakelsesmengde),
                ),
            )

            val journalpostDeltakelsesmengde = app.journalforingstatusRepository.get(hendelseDeltakelsesmengde.id)!!
            val journalpostForleng = app.journalforingstatusRepository.get(hendelseForleng.id)!!

            journalpostDeltakelsesmengde.journalpostId shouldBe journalforingstatusDeltakelsesmengde.journalpostId
            journalpostDeltakelsesmengde.bestillingsId shouldNotBe null
            journalpostForleng.journalpostId shouldNotBe null
            journalpostForleng.journalpostId shouldNotBe journalpostDeltakelsesmengde.journalpostId
            journalpostForleng.bestillingsId shouldNotBe null
        }

    @Test
    fun `journalforOgDistribuerEndringsvedtak - ulik deltakerid - feiler`() = integrationTest { app, _ ->
        val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
            HendelseTypeData.endreDeltakelsesmengde(),
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        val hendelseForleng = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                app.journalforingService.journalforOgDistribuerEndringsvedtak(
                    listOf(
                        HendelseMedJournalforingstatus(
                            hendelse = hendelseForleng,
                            journalforingstatus = Journalforingstatus(hendelseForleng.id, null, null),
                        ),
                        HendelseMedJournalforingstatus(
                            hendelse = hendelseDeltakelsesmengde,
                            journalforingstatus = Journalforingstatus(hendelseDeltakelsesmengde.id, null, null),
                        ),
                    ),
                )
            }
        }
    }
}

private fun produce(hendelse: HendelseDto) = produceStringString(
    ProducerRecord(Environment.DELTAKER_HENDELSE_TOPIC, hendelse.deltaker.id.toString(), objectMapper.writeValueAsString(hendelse)),
)
