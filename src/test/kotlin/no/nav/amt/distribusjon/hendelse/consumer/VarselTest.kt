package no.nav.amt.distribusjon.hendelse.consumer

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.time.delay
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.TestApp
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.HendelseDto
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.utils.assertProduced
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.utils.data.Varselsdata
import no.nav.amt.distribusjon.utils.produceStringString
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.model.beskjedTekst
import no.nav.amt.distribusjon.varsel.model.innbyggerDeltakerUrl
import no.nav.amt.distribusjon.varsel.model.oppgaveTekst
import no.nav.amt.distribusjon.varsel.nowUTC
import no.nav.amt.distribusjon.varsel.skalVarslesEksternt
import no.nav.amt.lib.models.hendelse.HendelseType
import no.nav.amt.lib.testing.AsyncUtils
import no.nav.amt.lib.testing.shouldBeCloseTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

class VarselTest {
    @Test
    fun `opprettUtkast - oppretter nytt varsel og produserer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.opprettUtkast())

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()

            varsel.aktivTil shouldBe null
            varsel.tekst shouldBe oppgaveTekst(hendelse.toModel(Distribusjonskanal.DITT_NAV, false))
            varsel.aktivFra shouldBeCloseTo nowUTC()
            varsel.deltakerId shouldBe hendelse.deltaker.id
            varsel.personident shouldBe hendelse.deltaker.personident
            varsel.erEksterntVarsel shouldBe hendelse.skalVarslesEksternt()

            assertProducedOppgave(varsel.id)
        }
    }

    @Test
    fun `opprettUtkast - hendelsen er håndtert tidligere - sender ikke nytt varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.opprettUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            hendelser = listOf(hendelse.id),
            aktivFra = nowUTC().minusMinutes(30),
            aktivTil = nowUTC().minusMinutes(20),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)

        produce(hendelse)

        AsyncUtils.eventually {
            val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()
            varsel.erAktiv shouldBe false
            assertNotProduced(varsel.id)
        }
    }

    @Test
    fun `navGodkjennUtkast - innbyggers distribusjonskanal er ikke digital - oppretter ikke varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.navGodkjennUtkast(), distribusjonskanal = Distribusjonskanal.PRINT)

        app.varselService.handleHendelse(hendelse)

        val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.BESKJED).getOrNull()
        varsel shouldBe null
    }

    @Test
    fun `navGodkjennUtkast - innbyggers er under manuell oppfolging - oppretter ikke varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(
            HendelseTypeData.navGodkjennUtkast(),
            distribusjonskanal = Distribusjonskanal.SDP,
            manuellOppfolging = true,
        )

        app.varselService.handleHendelse(hendelse)

        val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.BESKJED).getOrNull()
        varsel shouldBe null
    }

    @Test
    fun `avbrytUtkast - varsel er aktivt - inaktiverer varsel og produserer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.avbrytUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            Varsel.Status.AKTIV,
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
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.innbyggerGodkjennUtkast())
        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            Varsel.Status.AKTIV,
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
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.navGodkjennUtkast())
        produce(hendelse)
        AsyncUtils.eventually {
            assertNyBeskjed(app, hendelse, nowUTC())
        }
    }

    @Test
    fun `navGodkjennUtkast - tidligere varsel - inaktiverer varsel og oppretter beskjed`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.navGodkjennUtkast())

        val forrigeVarsel = Varselsdata.varsel(
            Varsel.Type.OPPGAVE,
            Varsel.Status.AKTIV,
            aktivFra = nowUTC().minusDays(1),
            deltakerId = hendelse.deltaker.id,
        )
        app.varselRepository.upsert(forrigeVarsel)
        produce(hendelse)

        AsyncUtils.eventually {
            assertNyBeskjed(app, hendelse, nowUTC())

            val inaktivertVarsel = app.varselRepository.get(forrigeVarsel.id).getOrThrow()
            inaktivertVarsel.aktivTil!! shouldBeCloseTo nowUTC()

            assertProducedInaktiver(forrigeVarsel.id)
        }
    }

    @Test
    fun `avsluttDeltakelse - nytt varsel med ekstern varsling, tidligere varsel skal revarsles - stopper revarsling av tidligere varsel`() =
        integrationTest { app, _ ->
            val deltakerId = UUID.randomUUID()
            val hendelse = Hendelsesdata.hendelse(HendelseTypeData.avsluttDeltakelse(), deltaker = Hendelsesdata.deltaker(deltakerId))

            val forrigeVarsel = Varselsdata.beskjed(
                Varsel.Status.INAKTIVERT,
                deltakerId = deltakerId,
                aktivFra = nowUTC().minusDays(6),
                aktivTil = nowUTC().plusDays(3),
                revarsles = nowUTC().plusDays(1),
            )

            app.varselRepository.upsert(forrigeVarsel)
            app.varselService.handleHendelse(hendelse)

            app.varselRepository
                .get(forrigeVarsel.id)
                .getOrThrow()
                .revarsles shouldBe null
        }

    @Test
    fun `endreSluttdato - ingen tidligere varsel - oppretter forsinket varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.endreSluttdato())
        produce(hendelse)
        AsyncUtils.eventually { assertNyBeskjed(app, hendelse, Varsel.nesteUtsendingstidspunkt()) }
    }

    @Test
    fun `endreStartdato - ingen tidligere varsel - oppretter forsinket varsel`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.endreStartdato())
        produce(hendelse)
        AsyncUtils.eventually { assertNyBeskjed(app, hendelse, Varsel.nesteUtsendingstidspunkt()) }
    }

    @Test
    fun `deltakerSistBesokt - aktiv beskjed - inaktiverer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.sistBesokt())
        val varsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.AKTIV,
            deltakerId = hendelse.deltaker.id,
            aktivFra = nowUTC().minusMinutes(1),
        )

        app.varselRepository.upsert(varsel)
        produce(hendelse)

        AsyncUtils.eventually {
            val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
            oppdatertVarsel.aktivTil!! shouldBeCloseTo nowUTC()
        }
        assertProducedInaktiver(varsel.id)
    }

    @Test
    fun `deltakerSistBesokt - beskjed venter på å bli sendt - inaktiverer`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.sistBesokt())
        val varsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.VENTER_PA_UTSENDELSE,
            deltakerId = hendelse.deltaker.id,
            aktivFra = nowUTC().plusMinutes(10),
        )

        app.varselRepository.upsert(varsel)
        produce(hendelse)

        AsyncUtils.eventually(Duration.ofSeconds(10)) {
            val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
            oppdatertVarsel.status shouldBe Varsel.Status.UTFORT
            oppdatertVarsel.aktivFra shouldBeCloseTo nowUTC()
            oppdatertVarsel.aktivTil!! shouldBeCloseTo nowUTC()
        }
    }

    @Test
    fun `deltakerSistBesokt - to beskjeder, en aktiv og en venter - inaktiverer begge`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.sistBesokt())
        val aktivtVarsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.AKTIV,
            deltakerId = hendelse.deltaker.id,
            aktivFra = nowUTC().minusMinutes(1),
        )
        val ventendeVarsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.VENTER_PA_UTSENDELSE,
            deltakerId = hendelse.deltaker.id,
            aktivFra = nowUTC().plusMinutes(10),
        )
        app.varselRepository.upsert(aktivtVarsel)
        app.varselRepository.upsert(ventendeVarsel)

        produce(hendelse)

        AsyncUtils.eventually {
            val oppdatertAktivtVarsel = app.varselRepository.get(aktivtVarsel.id).getOrThrow()
            oppdatertAktivtVarsel.status shouldBe Varsel.Status.UTFORT
            oppdatertAktivtVarsel.aktivFra shouldBeCloseTo aktivtVarsel.aktivFra
            oppdatertAktivtVarsel.aktivTil!! shouldBeCloseTo nowUTC()

            val oppdatertVentendeVarsel = app.varselRepository.get(ventendeVarsel.id).getOrThrow()
            oppdatertVentendeVarsel.status shouldBe Varsel.Status.UTFORT
            oppdatertVentendeVarsel.aktivFra shouldBeCloseTo nowUTC()
            oppdatertVentendeVarsel.aktivTil!! shouldBeCloseTo nowUTC()
        }
        assertProducedInaktiver(aktivtVarsel.id)
    }

    @Test
    fun `deltakerSistBesokt - siste besøk er før beskjed var sendt - inaktiverer ikke`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.sistBesokt(sistBesokt = ZonedDateTime.now().minusMinutes(10)))
        val varsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.AKTIV,
            deltakerId = hendelse.deltaker.id,
            aktivFra = nowUTC(),
            aktivTil = nowUTC().plus(Varsel.beskjedAktivLengde),
        )

        app.varselRepository.upsert(varsel)
        app.varselService.handleHendelse(hendelse)

        val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
        oppdatertVarsel.status shouldBe Varsel.Status.AKTIV
        oppdatertVarsel.aktivFra shouldBeCloseTo varsel.aktivFra
        oppdatertVarsel.aktivTil!! shouldBeCloseTo varsel.aktivTil
    }

    @Test
    fun `deltakerSistBesokt - siste besøk er før beskjed - inaktiverer ikke`() = integrationTest { app, _ ->
        val hendelse = Hendelsesdata.hendelse(HendelseTypeData.sistBesokt(sistBesokt = ZonedDateTime.now().minusMinutes(10)))
        val varsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.VENTER_PA_UTSENDELSE,
            deltakerId = hendelse.deltaker.id,
            aktivFra = nowUTC().plusMinutes(30),
            aktivTil = nowUTC().plus(Varsel.beskjedAktivLengde),
        )

        app.varselRepository.upsert(varsel)
        app.varselService.handleHendelse(hendelse)

        val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
        oppdatertVarsel.aktivFra shouldBeCloseTo varsel.aktivFra
        oppdatertVarsel.aktivTil!! shouldBeCloseTo varsel.aktivTil
    }

    private fun assertNyBeskjed(
        app: TestApp,
        hendelse: HendelseDto,
        aktivFra: ZonedDateTime,
    ) {
        val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.BESKJED).getOrThrow()

        varsel.aktivTil!! shouldBeCloseTo Varsel.nesteUtsendingstidspunkt().plus(Varsel.beskjedAktivLengde)
        varsel.tekst shouldBe beskjedTekst(hendelse.toModel(Distribusjonskanal.DITT_NAV, false))
        varsel.aktivFra shouldBeCloseTo aktivFra
        varsel.deltakerId shouldBe hendelse.deltaker.id
        varsel.personident shouldBe hendelse.deltaker.personident

        varsel.erEksterntVarsel shouldBe hendelse.skalVarslesEksternt()

        if (varsel.erAktiv) {
            val forventetUrl = innbyggerDeltakerUrl(varsel.deltakerId, hendelse.payload !is HendelseType.NavGodkjennUtkast)
            assertProducedBeskjed(varsel.id, forventetUrl)
        }
    }
}

private fun produce(hendelse: HendelseDto) = produceStringString(
    ProducerRecord(Environment.DELTAKER_HENDELSE_TOPIC, hendelse.deltaker.id.toString(), objectMapper.writeValueAsString(hendelse)),
)

private fun assertNotProduced(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) { cache ->
    delay(Duration.ofMillis(1000))
    cache[id] shouldBe null
}

fun assertProducedInaktiver(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) {
    AsyncUtils.eventually {
        val json = objectMapper.readTree(it[id])
        json["varselId"].asText() shouldBe id.toString()
        json["@event_name"].asText() shouldBe "inaktiver"
    }
}

fun assertProducedOppgave(id: UUID) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) {
    AsyncUtils.eventually {
        val json = objectMapper.readTree(it[id])
        json["varselId"].asText() shouldBe id.toString()
        json["@event_name"].asText() shouldBe "opprett"
        json["type"].asText() shouldBe "oppgave"
    }
}

fun assertProducedBeskjed(id: UUID, forventetUrl: String) = assertProduced(Environment.MINSIDE_VARSEL_TOPIC) {
    AsyncUtils.eventually {
        val json = objectMapper.readTree(it[id])
        json["varselId"].asText() shouldBe id.toString()
        json["@event_name"].asText() shouldBe "opprett"
        json["type"].asText() shouldBe "beskjed"
        json["link"].asText() shouldBe forventetUrl
    }
}

fun HendelseDto.skalVarslesEksternt() = this.toModel(Distribusjonskanal.DITT_NAV, false).skalVarslesEksternt()
