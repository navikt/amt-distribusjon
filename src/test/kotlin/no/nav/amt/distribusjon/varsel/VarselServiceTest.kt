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
    fun `sendRevarsler - inaktivert beskjed skal revarsles - oppretter og sender revarsel`() = integrationTest { app, _ ->
        val skalRevarsles = Varselsdata.beskjed(
            Varsel.Status.INAKTIVERT,
            aktivFra = nowUTC().minusDays(7).plusMinutes(1),
            revarsles = nowUTC().minusMinutes(1),
        )

        app.varselRepository.upsert(skalRevarsles)

        app.varselService.sendRevarsler()

        app.varselRepository.get(skalRevarsles.id).getOrThrow().revarsles shouldBe null

        val revarsel = app.varselRepository.getAktivt(skalRevarsles.deltakerId).getOrThrow()
        revarsel.erRevarsel shouldBe true
        revarsel.kanRevarsles shouldBe false
        revarsel.aktivFra shouldBeCloseTo nowUTC()
        revarsel.aktivTil!! shouldBeCloseTo nowUTC().plus(Varsel.beskjedAktivLengde)
        revarsel.revarselForVarsel shouldBe skalRevarsles.id

        assertProducedBeskjed(revarsel.id)
    }

    @Test
    fun `sendRevarsler - aktiv beskjed skal revarsles - inaktiverer beskjed, oppretter og sender revarsel`() = integrationTest { app, _ ->
        val skalRevarsles = Varselsdata.beskjed(
            Varsel.Status.AKTIV,
            aktivFra = nowUTC().minusDays(7).plusMinutes(1),
            revarsles = nowUTC().minusMinutes(1),
        )

        app.varselRepository.upsert(skalRevarsles)

        app.varselService.sendRevarsler()

        val oppdatertVarsel = app.varselRepository.get(skalRevarsles.id).getOrThrow()
        oppdatertVarsel.revarsles shouldBe null
        oppdatertVarsel.status shouldBe Varsel.Status.INAKTIVERT
        oppdatertVarsel.aktivTil!! shouldBeCloseTo nowUTC()

        assertProducedInaktiver(oppdatertVarsel.id)

        val revarsel = app.varselRepository.getAktivt(skalRevarsles.deltakerId).getOrThrow()
        revarsel.erRevarsel shouldBe true
        revarsel.kanRevarsles shouldBe false
        revarsel.aktivFra shouldBeCloseTo nowUTC()
        revarsel.aktivTil!! shouldBeCloseTo nowUTC().plus(Varsel.beskjedAktivLengde)
        revarsel.revarselForVarsel shouldBe skalRevarsles.id

        assertProducedBeskjed(revarsel.id)
    }

    @Test
    fun `sendRevarsler - aktiv beskjed skal ikke revarsles enda - endrer ingenting`() = integrationTest { app, _ ->
        val skalIkkeRevarsles = Varselsdata.beskjed(
            Varsel.Status.AKTIV,
            aktivFra = nowUTC().minusDays(6).plusMinutes(1),
            revarsles = nowUTC().plusDays(1),
        )

        app.varselRepository.upsert(skalIkkeRevarsles)

        app.varselService.sendRevarsler()

        val ikkeOppdatertVarsel = app.varselRepository.get(skalIkkeRevarsles.id).getOrThrow()
        ikkeOppdatertVarsel.revarsles!! shouldBeCloseTo skalIkkeRevarsles.revarsles!!
        ikkeOppdatertVarsel.status shouldBe skalIkkeRevarsles.status
        ikkeOppdatertVarsel.aktivTil!! shouldBeCloseTo skalIkkeRevarsles.aktivTil
    }
}
