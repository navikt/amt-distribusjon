package no.nav.amt.distribusjon.tiltakshendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.time.delay
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.distribusjon.utils.MockResponseHandler
import no.nav.amt.distribusjon.utils.assertProduced
import no.nav.amt.distribusjon.utils.data.DeltakerData
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.lib.models.arrangor.melding.EndringAarsak
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TiltakshendelseServiceTest {
    @Test
    fun `handleHendelse - nytt utkast - oppretter aktiv tiltakshendelse`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())

        app.tiltakshendelseService.handleHendelse(hendelse)

        val tiltakshendelse = app.tiltakshendelseRepository.getByHendelseId(hendelse.id).getOrThrow()

        tiltakshendelse.aktiv shouldBe true
        tiltakshendelse.deltakerId shouldBe hendelse.deltaker.id
        tiltakshendelse.forslagId shouldBe null
        tiltakshendelse.personident shouldBe hendelse.deltaker.personident
        tiltakshendelse.hendelser shouldBe listOf(hendelse.id)
        tiltakshendelse.type shouldBe Tiltakshendelse.Type.UTKAST
        tiltakshendelse.tekst shouldBe TiltakshendelseService.UTKAST_TIL_PAMELDING_TEKST
        tiltakshendelse.opprettet shouldBeCloseTo hendelse.opprettet
        tiltakshendelse.tiltakstype shouldBe hendelse.deltaker.deltakerliste.tiltak.type

        assertProducedTiltakshendelse(tiltakshendelse.id)
    }

    @Test
    fun `handleHendelse - utkast godkjent av nav - inaktiverer tiltakshendelse`() {
        testInaktiveringAvTiltakshendelse(HendelseTypeData.navGodkjennUtkast())
    }

    @Test
    fun `handleHendelse - utkast godkjent av innbygger - inaktiverer tiltakshendelse`() {
        testInaktiveringAvTiltakshendelse(HendelseTypeData.innbyggerGodkjennUtkast())
    }

    @Test
    fun `handleHendelse - utkast avbrutt - inaktiverer tiltakshendelse`() {
        testInaktiveringAvTiltakshendelse(HendelseTypeData.avbrytUtkast())
    }

    @Test
    fun `handleHendelse - utkast er håndtert - håndterer ikke på nytt`() = integrationTest { app, _ ->
        val opprettHendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())
        app.tiltakshendelseRepository.upsert(opprettHendelse.toTiltakshendelse().copy(aktiv = false))

        app.tiltakshendelseService.handleHendelse(opprettHendelse)

        val tiltakshendelse = app.tiltakshendelseRepository.getByHendelseId(opprettHendelse.id).getOrThrow()

        tiltakshendelse.aktiv shouldBe false
        assertNotProduced(tiltakshendelse.id)
    }

    @Test
    fun `handleHendelse - ny ForlengDeltakelse venter på svar - oppretter ny tiltakshendelse`() = integrationTest { app, _ ->
        val forslag = Forslag(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            LocalDateTime.now(),
            "begrunnelse",
            Forslag.ForlengDeltakelse(LocalDate.now()),
            Forslag.Status.VenterPaSvar,
        )

        app.tiltakshendelseService.handleForslag(forslag)
        val tiltakshendelse = app.tiltakshendelseRepository.getForslagHendelse(forslag.id).getOrThrow()

        tiltakshendelse.hendelser shouldBe emptyList()
        tiltakshendelse.tekst shouldBe "Forslag: Forleng deltakelse"
        tiltakshendelse.tiltakstype shouldBe Tiltakstype.ArenaKode.ARBFORB
        tiltakshendelse.aktiv shouldBe true
    }

    @Test
    fun `handleHendelse - ny ForlengDeltakelse godkjennes - oppretter ny tiltakshendelsee`() = integrationTest { app, _ ->
        val deltaker = DeltakerData.lagDeltaker()
        val forslag = Forslag(
            UUID.randomUUID(),
            deltaker.id,
            UUID.randomUUID(),
            LocalDateTime.now(),
            "begrunnelse",
            Forslag.ForlengDeltakelse(LocalDate.now()),
            Forslag.Status.VenterPaSvar,
        )

        MockResponseHandler.addDeltakerResponse(deltaker)

        app.tiltakshendelseService.handleForslag(forslag)

        val godkjentForslag = forslag.copy(
            status = Forslag.Status.Godkjent(Forslag.NavAnsatt(UUID.randomUUID(), UUID.randomUUID()), LocalDateTime.now()),
        )

        app.tiltakshendelseService.handleForslag(godkjentForslag)

        val tiltakhendelseFerdig = app.tiltakshendelseRepository.getForslagHendelse(forslag.id).getOrThrow()

        tiltakhendelseFerdig.aktiv shouldBe false
    }

    @Test
    fun `handleHendelse - Flere hendelser på samme bruker - oppretter nye tiltakshendelsee`() = integrationTest { app, _ ->
        val deltaker = DeltakerData.lagDeltaker()
        val forslag1 = Forslag(
            UUID.randomUUID(),
            deltaker.id,
            UUID.randomUUID(),
            LocalDateTime.now(),
            "begrunnelse",
            Forslag.ForlengDeltakelse(LocalDate.now()),
            Forslag.Status.VenterPaSvar,
        )

        val forslag2 = Forslag(
            UUID.randomUUID(),
            deltaker.id,
            UUID.randomUUID(),
            LocalDateTime.now(),
            "begrunnelse",
            Forslag.AvsluttDeltakelse(LocalDate.now(), EndringAarsak.FattJobb, null),
            Forslag.Status.VenterPaSvar,
        )

        MockResponseHandler.addDeltakerResponse(deltaker)

        app.tiltakshendelseService.handleForslag(forslag1)
        app.tiltakshendelseService.handleForslag(forslag2)

        val tiltakshendelse1 = app.tiltakshendelseRepository.getForslagHendelse(forslag1.id).getOrThrow()
        val tiltakshendelse2 = app.tiltakshendelseRepository.getForslagHendelse(forslag2.id).getOrThrow()

        tiltakshendelse1.forslagId shouldBe forslag1.id
        tiltakshendelse2.forslagId shouldBe forslag2.id

        val forslag1Godkjent = forslag1.copy(
            status = Forslag.Status.Godkjent(Forslag.NavAnsatt(UUID.randomUUID(), UUID.randomUUID()), LocalDateTime.now()),
        )
        app.tiltakshendelseService.handleForslag(forslag1Godkjent)

        val tiltakshendelse1Godkjent = app.tiltakshendelseRepository.getForslagHendelse(forslag1.id).getOrThrow()
        val tiltakshendelse2IkkeGodkjent = app.tiltakshendelseRepository.getForslagHendelse(forslag2.id).getOrThrow()

        tiltakshendelse1Godkjent.aktiv shouldBe false
        tiltakshendelse2IkkeGodkjent.aktiv shouldBe true
    }

    private fun testInaktiveringAvTiltakshendelse(hendelseType: HendelseType) = integrationTest { app, _ ->
        val opprettHendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())
        app.tiltakshendelseRepository.upsert(opprettHendelse.toTiltakshendelse())

        val godkjennHendelse = Hendelsesdata.hendelse(hendelseType, deltaker = opprettHendelse.deltaker)
        app.tiltakshendelseService.handleHendelse(godkjennHendelse)

        val tiltakshendelse = app.tiltakshendelseRepository.getByHendelseId(godkjennHendelse.id).getOrThrow()

        tiltakshendelse.aktiv shouldBe false
        assertProducedTiltakshendelse(tiltakshendelse.id)
    }

    private fun assertProducedTiltakshendelse(id: UUID) = assertProduced(Environment.TILTAKSHENDELSE_TOPIC) {
        AsyncUtils.eventually {
            it[id] shouldNotBe null
        }
    }

    private fun assertNotProduced(id: UUID) = assertProduced(Environment.TILTAKSHENDELSE_TOPIC) { cache ->
        delay(Duration.ofMillis(1000))
        cache[id] shouldBe null
    }
}
