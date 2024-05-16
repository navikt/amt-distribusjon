package no.nav.amt.distribusjon.journalforing

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.distribusjonskanal.skalDistribueresDigitalt
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient
import no.nav.amt.distribusjon.journalforing.dokdistfordeling.DokdistfordelingClient
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.pdf.lagEndringsvedtakPdfDto
import no.nav.amt.distribusjon.journalforing.pdf.lagHovedvedtakPdfDto
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.journalforing.sak.SakClient
import org.slf4j.LoggerFactory
import java.util.UUID

class JournalforingService(
    private val journalforingstatusRepository: JournalforingstatusRepository,
    private val amtPersonClient: AmtPersonClient,
    private val pdfgenClient: PdfgenClient,
    private val sakClient: SakClient,
    private val dokarkivClient: DokarkivClient,
    private val dokdistfordelingClient: DokdistfordelingClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun handleHendelse(hendelse: Hendelse) {
        val journalforingstatus = journalforingstatusRepository.get(hendelse.id)
        if (hendelseErBehandlet(journalforingstatus, hendelse.distribusjonskanal)) {
            log.info("Hendelse med id ${hendelse.id} for deltaker ${hendelse.deltaker.id} er allerede behandlet")
            return
        }
        when (hendelse.payload) {
            is HendelseType.InnbyggerGodkjennUtkast -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
                journalforingstatus,
            )
            is HendelseType.NavGodkjennUtkast -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
                journalforingstatus,
            )
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreSluttdato,
            is HendelseType.EndreStartdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.IkkeAktuell,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreBakgrunnsinformasjon,
            -> handleEndringsvedtak(hendelse, journalforingstatus)

            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            -> {
            }
        }
    }

    private suspend fun journalforHovedvedtak(
        hendelse: Hendelse,
        utkast: Utkast,
        journalforingstatus: Journalforingstatus?,
    ) {
        if (journalforingstatus == null || !journalforingstatus.erJournalfort()) {
            val veileder = when (hendelse.ansvarlig) {
                is HendelseAnsvarlig.NavVeileder -> hendelse.ansvarlig
            }
            val navBruker = amtPersonClient.hentNavBruker(hendelse.deltaker.personident)
            val aktivOppfolgingsperiode = navBruker.getAktivOppfolgingsperiode()
                ?: throw IllegalArgumentException(
                    "Kan ikke endre på deltaker ${hendelse.deltaker.id} som ikke har aktiv oppfølgingsperiode",
                )
            val sak = sakClient.opprettEllerHentSak(aktivOppfolgingsperiode.id)
            val pdf = pdfgenClient.hovedvedtak(
                lagHovedvedtakPdfDto(hendelse.deltaker, navBruker, utkast, veileder, hendelse.opprettet.toLocalDate()),
            )

            val journalpostId = dokarkivClient.opprettJournalpost(
                hendelseId = hendelse.id,
                fnr = hendelse.deltaker.personident,
                sak = sak,
                pdf = pdf,
                journalforendeEnhet = veileder.enhet.enhetsnummer,
                tiltakstype = hendelse.deltaker.deltakerliste.tiltak,
                endring = false,
            )

            val nyJournalforingstatus = Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = journalpostId,
                bestillingsId = null,
            )
            journalforingstatusRepository.upsert(nyJournalforingstatus)

            distribuer(hendelse, journalpostId)
        } else {
            distribuer(hendelse, journalforingstatus.journalpostId!!)
        }

        log.info("Journalførte hovedvedtak for deltaker ${hendelse.deltaker.id}")
    }

    private fun handleEndringsvedtak(hendelse: Hendelse, journalforingstatus: Journalforingstatus?) {
        if (hendelse.deltaker.forsteVedtakFattet == null) {
            log.error("Deltaker med id ${hendelse.deltaker.id} mangler fattet-dato for første vedtak")
            throw IllegalStateException("Kan ikke journalføre endringsvedtak hvis opprinnelig vedtak ikke er fattet")
        }
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = journalforingstatus?.journalpostId,
                bestillingsId = journalforingstatus?.bestillingsId,
            ),
        )
        log.info("Endringsvedtak for hendelse ${hendelse.id} er lagret og plukkes opp av asynkron jobb")
    }

    suspend fun journalforEndringsvedtak(hendelser: List<Hendelse>) {
        if (hendelser.isEmpty()) {
            return
        }
        val nyesteHendelse = hendelser.maxBy { it.opprettet }

        if (hendelser.find { it.deltaker.id != nyesteHendelse.deltaker.id } != null) {
            throw IllegalArgumentException("Alle hendelser må tilhøre samme deltaker!")
        }
        val veileder = when (nyesteHendelse.ansvarlig) {
            is HendelseAnsvarlig.NavVeileder -> nyesteHendelse.ansvarlig
        }
        val navBruker = amtPersonClient.hentNavBruker(nyesteHendelse.deltaker.personident)
        val aktivOppfolgingsperiode = navBruker.getAktivOppfolgingsperiode()
            ?: throw IllegalArgumentException(
                "Kan ikke endre på deltaker ${nyesteHendelse.deltaker.id} som ikke har aktiv oppfølgingsperiode",
            )
        val sak = sakClient.opprettEllerHentSak(aktivOppfolgingsperiode.id)
        val pdf = pdfgenClient.endringsvedtak(
            lagEndringsvedtakPdfDto(
                nyesteHendelse.deltaker,
                navBruker,
                veileder,
                hendelser,
                nyesteHendelse.opprettet.toLocalDate(),
            ),
        )

        val journalpostId = dokarkivClient.opprettJournalpost(
            hendelseId = nyesteHendelse.id,
            fnr = nyesteHendelse.deltaker.personident,
            sak = sak,
            pdf = pdf,
            journalforendeEnhet = veileder.enhet.enhetsnummer,
            tiltakstype = nyesteHendelse.deltaker.deltakerliste.tiltak,
            endring = true,
        )
        val hendelseIder = hendelser.map { it.id }

        val bestillingsId = distribuer(nyesteHendelse, journalpostId)

        hendelseIder.forEach {
            journalforingstatusRepository.upsert(
                Journalforingstatus(
                    hendelseId = it,
                    journalpostId = journalpostId,
                    bestillingsId = bestillingsId,
                ),
            )
        }

        log.info(
            "Journalførte endringsvedtak for deltaker ${hendelser.first().deltaker.id}, " +
                "hendelser ${hendelser.map { it.id }.joinToString()}",
        )
    }

    private suspend fun distribuer(hendelse: Hendelse, journalpostId: String): UUID? {
        if (!hendelse.distribusjonskanal.skalDistribueresDigitalt()) {
            val bestillingsId = dokdistfordelingClient.distribuerJournalpost(journalpostId)
            journalforingstatusRepository.upsert(
                Journalforingstatus(
                    hendelseId = hendelse.id,
                    journalpostId = journalpostId,
                    bestillingsId = bestillingsId,
                ),
            )
            return bestillingsId
        }
        return null
    }

    private fun hendelseErBehandlet(journalforingstatus: Journalforingstatus?, distribusjonskanal: Distribusjonskanal): Boolean {
        return journalforingstatus != null && journalforingstatus.erJournalfort() && journalforingstatus.erDistribuert(distribusjonskanal)
    }
}
