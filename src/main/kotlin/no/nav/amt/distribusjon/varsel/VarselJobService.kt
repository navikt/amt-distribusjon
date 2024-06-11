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
        navn = "SendVentendeVarslerJob",
        initialDelay = Duration.ofMinutes(2),
        period = Duration.ofMinutes(5),
    ) {
        varselService.sendVentendeVarsler()
    }

    private fun sendBrevJob() = jobManager.startJob(
        navn = "SendBrevJob",
        initialDelay = Duration.ofHours(1),
        period = Duration.ofHours(1),
    ) {
        val varsler = varselService.getVarslerSomSkalRevarsles()
    }
}
