package no.nav.amt.distribusjon.varsel

import no.nav.amt.distribusjon.utils.job.JobManager
import java.time.Duration

class VarselJobService(
    private val jobManager: JobManager,
    private val varselService: VarselService,
) {
    fun startJobs() {
        sendVentendeVarslerJob()
    }

    private fun sendVentendeVarslerJob() = jobManager.startJob(
        navn = "VentendeVarslerJob",
        initialDelay = Duration.ofMinutes(2),
        period = Duration.ofMinutes(5),
    ) {
        varselService.sendVentendeVarsler()
    }
}
