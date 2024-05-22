package no.nav.amt.distribusjon.varsel

import io.ktor.util.Attributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.journalforing.job.leaderelection.LeaderElection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class VarselJob(
    private val leaderElection: LeaderElection,
    private val attributes: Attributes,
    private val varselService: VarselService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startJob(): Timer {
        return fixedRateTimer(
            name = this.javaClass.simpleName,
            initialDelay = Duration.ofMinutes(2).toMillis(),
            period = Duration.ofMinutes(5).toMillis(),
        ) {
            scope.launch {
                if (leaderElection.isLeader() && attributes.getOrNull(isReadyKey) == true) {
                    try {
                        log.info("Kjører jobb for å sende varsler")
                        varselService.sendVentendeVarsler()
                    } catch (e: Exception) {
                        log.error("Noe gikk galt ved behandling av varsler", e)
                    }
                }
            }
        }
    }
}
