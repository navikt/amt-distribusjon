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
        val varsel = Varselsdata.varsel(Varsel.Type.BESKJED, aktivFra = nowUTC().minusMinutes(5), erSendt = false)
        app.varselRepository.upsert(varsel)

        app.varselService.sendVentendeVarsler()

        assertProducedBeskjed(varsel.id)
        val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
        oppdatertVarsel.erSendt shouldBe true
        oppdatertVarsel.aktivFra shouldBeCloseTo nowUTC()
    }

    @Test
    fun `sendVentendeVarsler - varsler er ikke klare for sending - sender ikke`() = integrationTest { app, _ ->
        val varsel = Varselsdata.varsel(Varsel.Type.BESKJED, aktivFra = nowUTC().plusMinutes(5), erSendt = false)
        app.varselRepository.upsert(varsel)

        app.varselService.sendVentendeVarsler()

        val oppdatertVarsel = app.varselRepository.get(varsel.id).getOrThrow()
        oppdatertVarsel.erSendt shouldBe false
        oppdatertVarsel.aktivFra shouldBeCloseTo varsel.aktivFra
    }

    @Test
    fun `sendVentendeVarsler - varsler klar for sending, det finnes ett aktivt varsel fra før - inaktiverer og sender nytt`() =
        integrationTest { app, _ ->
            val deltakerId = UUID.randomUUID()
            val aktivtVarsel = Varselsdata.varsel(
                Varsel.Type.BESKJED,
                deltakerId = deltakerId,
                aktivFra = nowUTC().minusMinutes(35),
                erSendt = true,
            )
            app.varselRepository.upsert(aktivtVarsel)

            val nyttVarsel = Varselsdata.varsel(
                Varsel.Type.BESKJED,
                deltakerId = deltakerId,
                aktivFra = nowUTC().minusMinutes(5),
                erSendt = false,
            )

            app.varselRepository.upsert(nyttVarsel)

            app.varselService.sendVentendeVarsler()

            app.varselRepository.get(aktivtVarsel.id).getOrThrow().erAktiv shouldBe false

            val oppdatertVarsel = app.varselRepository.get(nyttVarsel.id).getOrThrow()
            oppdatertVarsel.erSendt shouldBe true
            oppdatertVarsel.aktivFra shouldBeCloseTo nowUTC()

            assertProducedInaktiver(aktivtVarsel.id)
            assertProducedBeskjed(nyttVarsel.id)
        }
}
