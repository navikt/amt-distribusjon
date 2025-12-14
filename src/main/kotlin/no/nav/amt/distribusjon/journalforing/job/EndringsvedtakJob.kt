package no.nav.amt.distribusjon.journalforing.job

import no.nav.amt.distribusjon.hendelse.HendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.lib.utils.job.JobManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Component
class EndringsvedtakJob(
    private val jobManager: JobManager,
    private val hendelseRepository: HendelseRepository,
    private val journalforingService: JournalforingService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun startJob() = jobManager.startJob(
        name = this.javaClass.simpleName,
        initialDelay = Duration.of(5, ChronoUnit.MINUTES),
        period = Duration.of(10, ChronoUnit.MINUTES),
    ) {
        val endringsvedtak = hendelseRepository
            .getIkkeJournalforteHendelser(LocalDateTime.now().minusMinutes(30))
            .filter { it.hendelse.erEndringsVedtakSomSkalJournalfores() }

        val endringsvedtakPrDeltaker = endringsvedtak.groupBy { it.hendelse.deltaker.id }

        endringsvedtakPrDeltaker.forEach { entry ->
            log.info("Behandler endringsvedtak for deltaker med id ${entry.key}")
            try {
                journalforingService.journalforOgDistribuerEndringsvedtak(entry.value)
            } catch (e: Exception) {
                log.error("Behandling av endringsvedtak for deltaker med id ${entry.key} feilet", e)
            }
        }
        log.info("Ferdig med å behandle ${endringsvedtak.size} endringsvedtak")
    }
}
