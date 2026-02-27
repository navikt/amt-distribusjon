package no.nav.amt.distribusjon.journalforing.job

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.hendelse.HendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.journalforing.model.HendelseMedJournalforingstatus
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.data.HendelseTypeData
import no.nav.amt.distribusjon.utils.data.Hendelsesdata
import no.nav.amt.lib.utils.job.JobManager
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class EndringsvedtakJobTest {
    @Test
    fun `journalforEndringsvedtak - journalforer og distribuerer endringsvedtak naar nyeste hendelse er eldre enn graceperiode`() {
        val jobManager = mockk<JobManager>(relaxed = true)
        val hendelseRepository = mockk<HendelseRepository>()
        val journalforingService = mockk<JournalforingService>()

        val deltakerIdA = UUID.randomUUID()
        val deltakerIdB = UUID.randomUUID()

        val hendelser = listOf(
            // Deltaker A: to endringsvedtak, begge eldre enn graceperiode => skal behandles samlet
            hendelseMedStatus(deltakerId = deltakerIdA, opprettet = LocalDateTime.now().minusMinutes(60)),
            hendelseMedStatus(deltakerId = deltakerIdA, opprettet = LocalDateTime.now().minusMinutes(40)),
            // Deltaker B: endringsvedtak men nyeste er innenfor graceperiode => skal ikke behandles
            hendelseMedStatus(deltakerId = deltakerIdB, opprettet = LocalDateTime.now().minusMinutes(10)),
            // Ikke-endringsvedtak (utkast) skal filtreres bort
            hendelseMedStatus(
                deltakerId = deltakerIdA,
                opprettet = LocalDateTime.now().minusMinutes(70),
                payload = HendelseTypeData.opprettUtkast(),
            ),
        )

        every { hendelseRepository.getIkkeJournalforteHendelser() } returns hendelser
        coEvery { journalforingService.journalforOgDistribuerEndringsvedtak(any()) } returns Unit

        val job = EndringsvedtakJob(jobManager, hendelseRepository, journalforingService)
        runBlocking { job.journalforEndringsvedtak() }

        coVerify(exactly = 1) {
            journalforingService.journalforOgDistribuerEndringsvedtak(
                match { liste ->
                    liste.size == 2 && liste.all { it.hendelse.deltaker.id == deltakerIdA }
                },
            )
        }
    }

    @Test
    fun `journalforEndringsvedtak - fortsetter med neste deltaker hvis journalforing feiler for en deltaker`() {
        val jobManager = mockk<JobManager>(relaxed = true)
        val hendelseRepository = mockk<HendelseRepository>()
        val journalforingService = mockk<JournalforingService>()

        val deltakerIdA = UUID.randomUUID()
        val deltakerIdB = UUID.randomUUID()

        val hendelser = listOf(
            hendelseMedStatus(deltakerId = deltakerIdA, opprettet = LocalDateTime.now().minusMinutes(60)),
            hendelseMedStatus(deltakerId = deltakerIdB, opprettet = LocalDateTime.now().minusMinutes(60)),
        )

        every { hendelseRepository.getIkkeJournalforteHendelser() } returns hendelser

        coEvery {
            journalforingService.journalforOgDistribuerEndringsvedtak(
                match {
                    it
                        .first()
                        .hendelse.deltaker.id == deltakerIdA
                },
            )
        } throws RuntimeException("Simulert feil")
        coEvery {
            journalforingService.journalforOgDistribuerEndringsvedtak(
                match {
                    it
                        .first()
                        .hendelse.deltaker.id == deltakerIdB
                },
            )
        } returns Unit

        val job = EndringsvedtakJob(jobManager, hendelseRepository, journalforingService)
        runBlocking { job.journalforEndringsvedtak() }

        // Skal forsøke begge deltakelsene selv om første feiler
        coVerify(exactly = 2) { journalforingService.journalforOgDistribuerEndringsvedtak(any()) }
    }

    @Test
    fun `startJob - starter jobb med forventet initialDelay og period`() {
        val jobManager = mockk<JobManager>(relaxed = true)
        val hendelseRepository = mockk<HendelseRepository>(relaxed = true)
        val journalforingService = mockk<JournalforingService>(relaxed = true)

        EndringsvedtakJob(jobManager, hendelseRepository, journalforingService).startJob()

        verify(exactly = 1) {
            jobManager.startJob(
                name = "EndringsvedtakJob",
                initialDelay = Duration.of(5, ChronoUnit.MINUTES),
                period = Duration.of(10, ChronoUnit.MINUTES),
                job = any(),
            )
        }
    }

    private fun hendelseMedStatus(
        deltakerId: UUID,
        opprettet: LocalDateTime,
        payload: no.nav.amt.lib.models.hendelse.HendelseType = HendelseTypeData.endreStartdato(),
    ): HendelseMedJournalforingstatus {
        val deltaker = Hendelsesdata.lagDeltaker(id = deltakerId)
        val hendelse = Hendelsesdata.hendelse(payload = payload, deltaker = deltaker, opprettet = opprettet)

        return HendelseMedJournalforingstatus(
            hendelse = hendelse,
            journalforingstatus = Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = null,
                bestillingsId = null,
                kanIkkeDistribueres = null,
                kanIkkeJournalfores = null,
            ),
        )
    }
}
