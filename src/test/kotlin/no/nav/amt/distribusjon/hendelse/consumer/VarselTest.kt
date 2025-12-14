package no.nav.amt.distribusjon.hendelse.consumer

import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.IntegrationTestBase
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.distribusjonskanal.DokdistkanalClient
import no.nav.amt.distribusjon.hendelse.model.HendelseDto
import no.nav.amt.distribusjon.hendelse.model.toModel
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.distribusjon.varsel.VarselRepository
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.model.oppgaveTekst
import no.nav.amt.distribusjon.varsel.skalVarslesEksternt
import no.nav.amt.distribusjon.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.lib.kafka.Producer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.Duration

class VarselTest(
    private val kafkaProducer: Producer<String, String>,
    private val varselRepository: VarselRepository,
    private val objectMapper: ObjectMapper,
    @MockkBean private val dokdistkanalClient: DokdistkanalClient,
    @MockkBean private val veilarboppfolgingClient: VeilarboppfolgingClient,
) : IntegrationTestBase() {
    @BeforeEach
    fun beforeEach() {
        every { dokdistkanalClient.bestemDistribusjonskanal(any(), any()) } returns Distribusjonskanal.DITT_NAV
        every { veilarboppfolgingClient.erUnderManuellOppfolging(any()) } returns false
    }

    @Test
    fun `opprettUtkast - oppretter nytt varsel og produserer`() {
        val hendelse = Hendelsesdata.hendelseDto(HendelseTypeData.opprettUtkast())

        kafkaProducer.produce(
            topic = Environment.DELTAKER_HENDELSE_TOPIC,
            key = hendelse.deltaker.id.toString(),
            value = objectMapper.writeValueAsString(hendelse),
        )

        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            val varsel = varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrNull()

            assertSoftly(varsel.shouldNotBeNull()) {
                tekst shouldBe oppgaveTekst(hendelse.toModel(Distribusjonskanal.DITT_NAV, false))
                /*
                                aktivFra shouldBeCloseTo nowUTC()
                                deltakerId shouldBe hendelse.deltaker.id
                                personident shouldBe hendelse.deltaker.personident
                                erEksterntVarsel shouldBe hendelse.skalVarslesEksternt()
                 */
            }
        }

        /*
                AsyncUtils.eventually {
                    val varsel = app.varselRepository.getSisteVarsel(hendelse.deltaker.id, Varsel.Type.OPPGAVE).getOrThrow()

                    varsel.aktivTil shouldBe null
                    varsel.tekst shouldBe oppgaveTekst(hendelse.toModel(Distribusjonskanal.DITT_NAV, false))
                    varsel.aktivFra shouldBeCloseTo nowUTC()
                    varsel.deltakerId shouldBe hendelse.deltaker.id
                    varsel.personident shouldBe hendelse.deltaker.personident
                    varsel.erEksterntVarsel shouldBe hendelse.skalVarslesEksternt()

                    app.assertProducedOppgave(varsel.id)
                }
         */
    }
    /*

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
                app.assertNotProduced(varsel.id)
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

                app.assertProducedInaktiver(varsel.id)
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

                app.assertProducedInaktiver(varsel.id)
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

                app.assertProducedInaktiver(forrigeVarsel.id)
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
            app.assertProducedInaktiver(varsel.id)
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
            app.assertProducedInaktiver(aktivtVarsel.id)
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
                app.assertProducedBeskjed(varsel.id, forventetUrl)
            }
        }
    }

    private fun TestApp.assertNotProduced(id: UUID) {
        this shouldNot haveOutboxRecord(id, Environment.MINSIDE_VARSEL_TOPIC)
    }

    fun TestApp.assertProducedInaktiver(id: UUID) {
        this should haveOutboxRecord(id, Environment.MINSIDE_VARSEL_TOPIC) {
            val json = it.value
            json["varselId"].asText() == id.toString() &&
                json["@event_name"].asText() == "inaktiver"
        }
    }

    fun TestApp.assertProducedOppgave(id: UUID) {
        this should haveOutboxRecord(id, Environment.MINSIDE_VARSEL_TOPIC) { record ->
            val json = record.value

            json["varselId"].asText() == id.toString() &&
                json["@event_name"].asText() == "opprett" &&
                json["type"].asText() == "oppgave"
        }
    }

    fun TestApp.assertProducedBeskjed(id: UUID, forventetUrl: String) {
        this should haveOutboxRecord(id, Environment.MINSIDE_VARSEL_TOPIC) { record ->
            val json = record.value
            json["varselId"].asText() == id.toString() &&
                json["@event_name"].asText() == "opprett" &&
                json["type"].asText() == "beskjed" &&
                json["link"].asText() == forventetUrl
        }
     */
}

fun HendelseDto.skalVarslesEksternt() = this.toModel(Distribusjonskanal.DITT_NAV, false).skalVarslesEksternt()
