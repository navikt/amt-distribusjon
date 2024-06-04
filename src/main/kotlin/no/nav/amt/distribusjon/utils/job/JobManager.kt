package no.nav.amt.distribusjon.utils.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.amt.distribusjon.utils.job.leaderelection.LeaderElection
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.concurrent.fixedRateTimer

class JobManager(
    private val leaderElection: LeaderElection,
    private val applicationIsReady: () -> Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun startJob(
        navn: String,
        initialDelay: Duration,
        period: Duration,
        job: suspend () -> Unit,
    ) = fixedRateTimer(
        name = navn,
        initialDelay = initialDelay.toMillis(),
        period = period.toMillis(),
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (leaderElection.isLeader() && applicationIsReady()) {
                try {
                    log.info("Kjører jobb: $navn")
                    job()
                } catch (e: Exception) {
                    log.error("Noe gikk galt med jobb: $navn", e)
                }
            }
        }
    }
}
