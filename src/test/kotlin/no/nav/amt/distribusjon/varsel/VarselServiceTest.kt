package no.nav.amt.distribusjon.varsel

import io.kotest.matchers.shouldBe
import no.nav.amt.distribusjon.hendelse.consumer.assertProducedBeskjed
import no.nav.amt.distribusjon.hendelse.consumer.assertProducedInaktiver
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.utils.data.Varselsdata
import no.nav.amt.distribusjon.utils.shouldBeCloseTo
import no.nav.amt.distribusjon.varsel.model.Varsel
import org.junit.Test
import java.util.UUID

class VarselServiceTest {
    @Test
    fun `sendVentendeVarsler - varsler er klare for sending - sender`() = integrationTest { app, _ ->
        val varsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.VENTER_PA_UTSENDELSE,
            aktivFra = nowUTC().minusMinutes(5),
        )
        app.varselRepository.upsert(varsel)

        app.varselService.sendVentendeVarsler()

        assertProducedBeskjed(varsel.id)
        val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
        oppdatertVarsel.aktivFra shouldBeCloseTo nowUTC()
    }

    @Test
    fun `sendVentendeVarsler - varsler er ikke klare for sending - sender ikke`() = integrationTest { app, _ ->
        val varsel = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.VENTER_PA_UTSENDELSE,
            aktivFra = nowUTC().plusMinutes(5),
        )
        app.varselRepository.upsert(varsel)

        app.varselService.sendVentendeVarsler()

        val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
        oppdatertVarsel.aktivFra shouldBeCloseTo varsel.aktivFra
    }

    @Test
    fun `sendVentendeVarsler - varsler klar for sending, det finnes ett aktivt varsel fra før - inaktiverer og sender nytt`() =
        integrationTest { app, _ ->
            val deltakerId = UUID.randomUUID()
            val aktivtVarsel = Varselsdata.varsel(
                Varsel.Type.BESKJED,
                Varsel.Status.AKTIV,
                deltakerId = deltakerId,
                aktivFra = nowUTC().minusMinutes(35),
            )
            app.varselRepository.upsert(aktivtVarsel)

            val nyttVarsel = Varselsdata.varsel(
                Varsel.Type.BESKJED,
                Varsel.Status.VENTER_PA_UTSENDELSE,
                deltakerId = deltakerId,
                aktivFra = nowUTC().minusMinutes(5),
            )

            app.varselRepository.upsert(nyttVarsel)

            app.varselService.sendVentendeVarsler()

            app.varselRepository.get(aktivtVarsel.id).getOrThrow().erAktiv shouldBe false

            val oppdatertVarsel = app.varselRepository.get(nyttVarsel.id).getOrThrow()
            oppdatertVarsel.aktivFra shouldBeCloseTo nowUTC()

            assertProducedInaktiver(aktivtVarsel.id)
            assertProducedBeskjed(nyttVarsel.id)
        }

    @Test
    fun `getVarselerSomSkalRevarsles - skal returnere alle varsler som er aktive og eldre enn frist`() = integrationTest { app, client ->
        fun aktivBeskjed(timer: Long) = Varselsdata.varsel(
            Varsel.Type.BESKJED,
            Varsel.Status.AKTIV,
            aktivFra = nowUTC().minusHours(timer).minusMinutes(1),
        )

        val skalIkkeReturneres = aktivBeskjed(39)
        val skalReturneres1 = aktivBeskjed(40)
        val skalReturneres2 = aktivBeskjed(41)

        app.varselRepository.upsert(skalIkkeReturneres)
        app.varselRepository.upsert(skalReturneres1)
        app.varselRepository.upsert(skalReturneres2)

        val varsler = app.varselService.getVarslerSomSkalRevarsles()

        varsler.size shouldBe 2
        varsler.any { it == skalReturneres1 } shouldBe true
        varsler.any { it == skalReturneres2 } shouldBe true
    }
}
