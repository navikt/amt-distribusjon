package no.nav.amt.distribusjon.journalforing

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.HendelseDto
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.AsyncUtils
import no.nav.amt.distribusjon.utils.MockResponseHandler
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.utils.data.Persondata
import no.nav.amt.distribusjon.utils.produceStringString
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
    fun `handleHendelse - InnbyggerGodkjennUtkast, er allerede journalfort - ignorerer hendelse`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast())

        val journalpostId = "12345"

        app.hendelseRepository.insert(hendelse)
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelse.id, journalpostId))

        app.journalforingService.handleHendelse(hendelse)

        val status = app.journalforingstatusRepository.get(hendelse.id)
        status!!.journalpostId shouldBe journalpostId
    }

    @Test
    fun `handleHendelse - AvsluttDeltakelse, er allerede journalfort - ignorerer hendelse`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.avsluttDeltakelse())

        val journalpostId = "12345"

        app.hendelseRepository.insert(hendelse)
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelse.id, journalpostId))

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
                app.journalforingService.handleHendelse(hendelse.toModel(Distribusjonskanal.DITT_NAV))
            }
        }
    }

    @Test
    fun `handleHendelse - EndreBakgrunnsinformasjon - journalforer ikke`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.endreBakgrunnsinformasjon())

        app.journalforingService.handleHendelse(hendelse)
        app.journalforingstatusRepository.get(hendelse.id) shouldBe null
    }

    @Test
    fun `journalforEndringsvedtak - deltakelsesmengde og forleng - journalforer endringsvedtak`() = integrationTest { app, _ ->
        val deltaker = Hendelsesdata.deltaker()

        val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
            HendelseTypeData.endreDeltakelsesmengde(),
            deltaker = deltaker,
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelseDeltakelsesmengde.id, null))
        val hendelseForleng = Hendelsesdata.hendelse(
            HendelseTypeData.forlengDeltakelse(),
            deltaker = deltaker,
            ansvarlig = hendelseDeltakelsesmengde.ansvarlig,
            opprettet = LocalDateTime.now(),
        )
        app.journalforingstatusRepository.upsert(Journalforingstatus(hendelseForleng.id, null))

        app.journalforingService.journalforEndringsvedtak(listOf(hendelseForleng, hendelseDeltakelsesmengde))

        val journalpostDeltakelsesmengde = app.journalforingstatusRepository.get(hendelseDeltakelsesmengde.id)!!
        val journalpostForleng = app.journalforingstatusRepository.get(hendelseForleng.id)!!

        journalpostDeltakelsesmengde.journalpostId shouldNotBe null
        journalpostDeltakelsesmengde.journalpostId shouldBe journalpostForleng.journalpostId
    }

    @Test
    fun `journalforEndringsvedtak - ulik deltakerid - feiler`() = integrationTest { app, _ ->
        val hendelseDeltakelsesmengde = Hendelsesdata.hendelse(
            HendelseTypeData.endreDeltakelsesmengde(),
            opprettet = LocalDateTime.now().minusMinutes(20),
        )
        val hendelseForleng = Hendelsesdata.hendelse(HendelseTypeData.forlengDeltakelse(), opprettet = LocalDateTime.now())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                app.journalforingService.journalforEndringsvedtak(listOf(hendelseForleng, hendelseDeltakelsesmengde))
            }
        }
    }
}

private fun produce(hendelse: HendelseDto) = produceStringString(
    ProducerRecord(Environment.DELTAKER_HENDELSE_TOPIC, hendelse.deltaker.id.toString(), objectMapper.writeValueAsString(hendelse)),
)
