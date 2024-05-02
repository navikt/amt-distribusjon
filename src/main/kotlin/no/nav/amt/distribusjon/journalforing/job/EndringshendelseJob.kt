package no.nav.amt.distribusjon.journalforing.job

import io.ktor.util.Attributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.journalforing.EndringshendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.journalforing.job.leaderelection.LeaderElection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class EndringshendelseJob(
    private val leaderElection: LeaderElection,
    private val attributes: Attributes,
    private val endringshendelseRepository: EndringshendelseRepository,
    private val journalforingService: JournalforingService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    fun startJob(): Timer {
        return fixedRateTimer(
            name = this.javaClass.simpleName,
            initialDelay = Duration.of(5, ChronoUnit.MINUTES).toMillis(),
            period = Duration.of(15, ChronoUnit.MINUTES).toMillis(),
        ) {
            scope.launch {
                if (leaderElection.isLeader() && attributes.getOrNull(isReadyKey) == true) {
                    try {
                        log.info("Kjører jobb for å behandle endringshendelser")
                        val endringshendelser = endringshendelseRepository.getHendelser(LocalDateTime.now().minusMinutes(30))
                        val hendelserPrDeltaker = endringshendelser.groupBy { it.deltakerId }
                        hendelserPrDeltaker.forEach { entry ->
                            log.info("Behandler endringshendelser for deltaker med id ${entry.key}")
                            journalforingService.journalforEndringsvedtak(entry.value.map { it.hendelse })
                        }
                        log.info("Ferdig med å behandle endringshendelser")
                    } catch (e: Exception) {
                        log.error("Noe gikk galt ved behandling av endringshendelser", e)
                    }
                }
            }
        }
    }
}
