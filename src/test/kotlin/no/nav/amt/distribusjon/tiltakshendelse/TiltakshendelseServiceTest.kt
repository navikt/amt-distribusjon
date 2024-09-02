package no.nav.amt.distribusjon.tiltakshendelse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.time.delay
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.distribusjon.utils.assertProduced
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.junit.Test
import java.time.Duration
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
        app.tiltakshendelseRepository.upsert(opprettHendelse.tilTiltakshendelse().copy(aktiv = false))

        app.tiltakshendelseService.handleHendelse(opprettHendelse)

        val tiltakshendelse = app.tiltakshendelseRepository.getByHendelseId(opprettHendelse.id).getOrThrow()

        tiltakshendelse.aktiv shouldBe false
        assertNotProduced(tiltakshendelse.id)
    }

    private fun testInaktiveringAvTiltakshendelse(hendelseType: HendelseType) = integrationTest { app, _ ->
        val opprettHendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())
        app.tiltakshendelseRepository.upsert(opprettHendelse.tilTiltakshendelse())

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
