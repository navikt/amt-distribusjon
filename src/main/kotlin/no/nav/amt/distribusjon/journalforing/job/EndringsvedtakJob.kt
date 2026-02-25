package no.nav.amt.distribusjon.journalforing.job

import no.nav.amt.distribusjon.hendelse.HendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.lib.utils.job.JobManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class EndringsvedtakJob(
    private val jobManager: JobManager,
    private val hendelseRepository: HendelseRepository,
    private val journalforingService: JournalforingService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun startJob() = jobManager.startJob(
        name = this.javaClass.simpleName,
        initialDelay = Duration.of(5, ChronoUnit.MINUTES),
        period = Duration.of(30, ChronoUnit.MINUTES),
    ) {
        val enHalvtimeSiden = LocalDateTime.now().minusMinutes(30)
        val endringsvedtak = hendelseRepository
            .getIkkeJournalforteHendelser(enHalvtimeSiden)
            .filter { it.hendelse.erEndringsVedtakSomSkalJournalfores() }

        val endringsvedtakPrDeltaker = endringsvedtak.groupBy { it.hendelse.deltaker.id }

        endringsvedtakPrDeltaker.forEach { (deltakerId, hendelser) ->
            /*
             * Prosesser kun hendelser for en deltaker hvis den nyeste hendelsen er eldre enn 30 minutter,
             *  for å samle alle endringer gjort i løpet av en halvtime i samme brev.
             * Dette for å unngå å sende flere endringsvedtak etter hverandre hvis det gjøres flere endringer på samme deltaker i løpet av kort tid.
             */
            val nyesteHendelseOpprettet = hendelser.maxByOrNull { it.hendelse.opprettet } ?: return@forEach
            if (nyesteHendelseOpprettet.hendelse.opprettet.isBefore(enHalvtimeSiden)) {
                log.info("Behandler ${hendelser.size} endringsvedtak for deltaker med id $deltakerId")
                try {
                    journalforingService.journalforOgDistribuerEndringsvedtak(hendelser)
                } catch (e: Exception) {
                    log.error("Behandling av endringsvedtak for deltaker med id $deltakerId feilet", e)
                }
            } else {
                log.info("Venter med å behandle endringsvedtak for deltaker $deltakerId (nyeste hendelse: $nyesteHendelseOpprettet)")
            }
        }
        log.info("Ferdig med å behandle ${endringsvedtak.size} endringsvedtak")
    }
}
