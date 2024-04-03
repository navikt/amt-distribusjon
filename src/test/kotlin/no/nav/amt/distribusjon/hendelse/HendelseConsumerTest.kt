package no.nav.amt.distribusjon.hendelse

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.time.delay
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.hendelse.model.Hendelse
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
    fun `opprettUtkast - oppretter nytt varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            varsel.aktivTil shouldBe null
            varsel.tekst shouldBe PAMELDING_TEKST
            varsel.aktivFra shouldBeCloseTo nowUTC().plusHours(1)
            varsel.deltakerId shouldBe hendelse.deltaker.id
            varsel.personident shouldBe hendelse.deltaker.personident
        }
    }

    @Test
    fun `opprettUtkast - varsel ikke sendt - utsetter varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.opprettUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.PAMELDING,
            aktivFra = nowUTC().plusMinutes(30),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil shouldBe null
            varsel.tekst shouldBe forrigeVarsel.tekst
            varsel.aktivFra shouldBeCloseTo nowUTC().plusHours(1)
            varsel.deltakerId shouldBe forrigeVarsel.deltakerId
            varsel.personident shouldBe forrigeVarsel.personident
        }
    }

    @Test
    fun `avbrytUtkast - varsel er aktivt - inaktiverer varsel og produserer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.avbrytUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.PAMELDING,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(varsel.id)
        }
    }

    @Test
    fun `avbrytUtkast - varsel er ikke sendt - inaktiverer varsel og produserer ikke`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.avbrytUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.PAMELDING,
            aktivFra = nowUTC().plusHours(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil!! shouldBeCloseTo nowUTC()
        }
        assertNotProduced(forrigeVarsel.id)
    }

    @Test
    fun `innbyggerGodkjennerUtkast - inaktiverer varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.innbyggerGodkjennUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.PAMELDING,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            varsel.id shouldBe forrigeVarsel.id
            varsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(varsel.id)
        }
    }

    @Test
    fun `navGodkjennUtkast - ingen tidligere varsel - oppretter beskjed`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.navGodkjennUtkast())

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            varsel.aktivTil!! shouldBeCloseTo nowUTC().plus(VarselService.beskjedAktivLengde)
            varsel.tekst shouldBe PLACEHOLDER_BESKJED_TEKST
            varsel.aktivFra shouldBeCloseTo nowUTC().plusHours(1)
            varsel.deltakerId shouldBe hendelse.deltaker.id
            varsel.personident shouldBe hendelse.deltaker.personident
        }
    }

    @Test
    fun `navGodkjennUtkast - tidligere varsel - inaktiverer varsel og oppretter beskjed`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.navGodkjennUtkast())

        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.PAMELDING,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val beskjed = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.PAMELDING).getOrThrow()

            beskjed.aktivTil!! shouldBeCloseTo nowUTC().plus(VarselService.beskjedAktivLengde)
            beskjed.tekst shouldBe PLACEHOLDER_BESKJED_TEKST
            beskjed.aktivFra shouldBeCloseTo nowUTC().plusHours(1)
            beskjed.deltakerId shouldBe hendelse.deltaker.id
            beskjed.personident shouldBe hendelse.deltaker.personident

            val inaktivertVarsel = app.varselRepository.get(forrigeVarsel.id).getOrThrow()
            inaktivertVarsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(forrigeVarsel.id)
        }
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
