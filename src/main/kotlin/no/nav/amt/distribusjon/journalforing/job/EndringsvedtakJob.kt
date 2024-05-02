package no.nav.amt.distribusjon.journalforing.job

import io.ktor.util.Attributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.hendelse.db.HendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.journalforing.job.leaderelection.LeaderElection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class EndringsvedtakJob(
    private val leaderElection: LeaderElection,
    private val attributes: Attributes,
    private val hendelseRepository: HendelseRepository,
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
                        log.info("Kjører jobb for å behandle endringsvedtak")
                        val endringsvedtak = hendelseRepository.getIkkeJournalforteHendelser(LocalDateTime.now().minusMinutes(30))
                            .filter { it.erEndringsVedtakSomSkalJournalfores() }
                        val endringsvedtakPrDeltaker = endringsvedtak.groupBy { it.deltakerId }
                        endringsvedtakPrDeltaker.forEach { entry ->
                            log.info("Behandler endringsvedtak for deltaker med id ${entry.key}")
                            journalforingService.journalforEndringsvedtak(entry.value)
                        }
                        log.info("Ferdig med å behandle endringsvedtak")
                    } catch (e: Exception) {
                        log.error("Noe gikk galt ved behandling av endringsvedtak", e)
                    }
                }
            }
        }
    }
}
