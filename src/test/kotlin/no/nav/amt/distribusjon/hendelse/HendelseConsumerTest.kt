package no.nav.amt.distribusjon.hendelse

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.time.delay
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.TestApp
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.utils.AsyncUtils
import no.nav.amt.distribusjon.utils.assertProduced
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.utils.data.Varselsdata
import no.nav.amt.distribusjon.utils.produceStringString
import no.nav.amt.distribusjon.utils.shouldBeCloseTo
import no.nav.amt.distribusjon.varsel.VarselService
import no.nav.amt.distribusjon.varsel.model.PAMELDING_TEKST
import no.nav.amt.distribusjon.varsel.model.PLACEHOLDER_BESKJED_TEKST
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.nowUTC
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Test
import java.time.Duration
import java.util.UUID

class HendelseConsumerTest {
    @Test
    fun `opprettUtkast - oppretter nytt varsel og produserer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()

            varsel.aktivTil shouldBe null
            varsel.tekst shouldBe PAMELDING_TEKST
            varsel.aktivFra shouldBeCloseTo nowUTC()
            varsel.deltakerId shouldBe hendelse.deltaker.id
            varsel.personident shouldBe hendelse.deltaker.personident
            varsel.skalVarsleEksternt shouldBe hendelse.skalVarslesEksternt()

            assertProducedOppgave(varsel.id)
        }
    }

    @Test
    fun `opprettUtkast - tidligere oppgave er aktiv - sender ikke nytt varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            aktivFra = nowUTC().minusMinutes(30),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil shouldBe null
            varsel.tekst shouldBe forrigeVarsel.tekst
            varsel.aktivFra shouldBeCloseTo forrigeVarsel.aktivFra
            varsel.deltakerId shouldBe forrigeVarsel.deltakerId
            varsel.personident shouldBe forrigeVarsel.personident
            varsel.skalVarsleEksternt shouldBe hendelse.skalVarslesEksternt()

            assertNotProduced(varsel.id)
        }
    }

    @Test
    fun `avbrytUtkast - varsel er aktivt - inaktiverer varsel og produserer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.avbrytUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(varsel.id)
        }
    }

    @Test
    fun `innbyggerGodkjennerUtkast - inaktiverer varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(varsel.id)
        }
    }

    @Test
    fun `navGodkjennUtkast - ingen tidligere varsel - oppretter beskjed`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.navGodkjennUtkast())
        produce(hendelse)
        AsyncUtils.eventually { assertNyBeskjed(app, hendelse) }
    }

    @Test
    fun `navGodkjennUtkast - tidligere varsel - inaktiverer varsel og oppretter beskjed`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.navGodkjennUtkast())

        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            assertNyBeskjed(app, hendelse)

            val inaktivertVarsel = app.varselRepository.get(forrigeVarsel.id).getOrThrow()
            inaktivertVarsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(forrigeVarsel.id)
        }
    }

    @Test
    fun `endreSluttdato - ingen tidligere varsel - oppretter varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.endreSluttdato())
        produce(hendelse)
        AsyncUtils.eventually { assertNyBeskjed(app, hendelse) }
    }

    @Test
    fun `endreStartdato - ingen tidligere varsel - oppretter varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.endreStartdato())
        produce(hendelse)
        AsyncUtils.eventually { assertNyBeskjed(app, hendelse) }
    }

    private fun assertNyBeskjed(app: TestApp, hendelse: Hendelse) {
        val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.BESKJED).getOrThrow()

        varsel.aktivTil!! shouldBeCloseTo nowUTC().plus(VarselService.beskjedAktivLengde)
        varsel.tekst shouldBe PLACEHOLDER_BESKJED_TEKST
        varsel.aktivFra shouldBeCloseTo nowUTC()
        varsel.deltakerId shouldBe hendelse.deltaker.id
        varsel.personident shouldBe hendelse.deltaker.personident

        varsel.skalVarsleEksternt shouldBe hendelse.skalVarslesEksternt()

        assertProducedBeskjed(varsel.id)
    }
}

private fun produce(hendelse: Hendelse) = produceStringString(
    ProducerRecord(Environment.DELTAKER_HENDELSE_TOPIC, hendelse.deltaker.id.toString(), objectMapper.writeValueAsString(hendelse)),
)

private fun assertNotProduced(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) { cache ->
    delay(Duration.ofMillis(1000))
    cache[id] shouldBe null
}

private fun assertProducedInaktiver(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) {
    AsyncUtils.eventually {
        val json = objectMapper.readTree(it[id])
        json["varselId"].asText() shouldBe id.toString()
        json["@event_name"].asText() shouldBe "inaktiver"
    }
}

private fun assertProducedOppgave(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) {
    AsyncUtils.eventually {
        val json = objectMapper.readTree(it[id])
        json["varselId"].asText() shouldBe id.toString()
        json["@event_name"].asText() shouldBe "opprett"
        json["type"].asText() shouldBe "oppgave"
    }
}

private fun assertProducedBeskjed(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) {
    AsyncUtils.eventually {
        val json = objectMapper.readTree(it[id])
        json["varselId"].asText() shouldBe id.toString()
        json["@event_name"].asText() shouldBe "opprett"
        json["type"].asText() shouldBe "beskjed"
    }
}

private fun Hendelse.skalVarslesEksternt() = when (payload) {
    is HendelseType.EndreBakgrunnsinformasjon,
    is HendelseType.EndreDeltakelsesmengde,
    is HendelseType.EndreInnhold,
    is HendelseType.EndreSluttarsak,
    is HendelseType.EndreStartdato,
    is HendelseType.InnbyggerGodkjennUtkast,
    -> false

    is HendelseType.ForlengDeltakelse,
    is HendelseType.AvbrytUtkast,
    is HendelseType.EndreSluttdato,
    is HendelseType.IkkeAktuell,
    is HendelseType.NavGodkjennUtkast,
    is HendelseType.OpprettUtkast,
    is HendelseType.AvsluttDeltakelse,
    -> true
}
