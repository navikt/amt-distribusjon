package no.nav.amt.distribusjon.journalforing

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
        if (hendelseErBehandlet(hendelse.id)) {
            log.info("Hendelse med id ${hendelse.id} for deltaker ${hendelse.deltaker.id} er allerede behandlet")
            return
        }
        when (hendelse.payload) {
            is HendelseType.InnbyggerGodkjennUtkast -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
            )
            is HendelseType.NavGodkjennUtkast -> journalforHovedvedtak(
                hendelse,
                hendelse.payload.utkast,
            )
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreSluttdato,
            is HendelseType.EndreStartdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.IkkeAktuell,
            -> handleEndringsvedtak(hendelse)

            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            -> {
            }
        }
    }

    private suspend fun journalforHovedvedtak(hendelse: Hendelse, utkast: Utkast) {
        val veileder = when (hendelse.ansvarlig) {
            is HendelseAnsvarlig.NavVeileder -> hendelse.ansvarlig
        }
        val navBruker = amtPersonClient.hentNavBruker(hendelse.deltaker.personident)
        val aktivOppfolgingsperiode = navBruker.getAktivOppfolgingsperiode()
            ?: throw IllegalArgumentException("Kan ikke endre på deltaker ${hendelse.deltaker.id} som ikke har aktiv oppfølgingsperiode")
        val sak = sakClient.opprettEllerHentSak(aktivOppfolgingsperiode.id)
        val pdf = pdfgenClient.hovedvedtak(lagHovedvedtakPdfDto(hendelse.deltaker, navBruker, utkast, veileder))

        val journalpostId = dokarkivClient.opprettJournalpost(
            hendelseId = hendelse.id,
            fnr = hendelse.deltaker.personident,
            sak = sak,
            pdf = pdf,
            journalforendeEnhet = veileder.enhet.enhetsnummer,
            tiltakstype = hendelse.deltaker.deltakerliste.tiltak,
            endring = false,
        )

        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = journalpostId,
            ),
        )

        distribuer(hendelse, journalpostId)

        log.info("Journalførte hovedvedtak for deltaker ${hendelse.deltaker.id}")
    }

    private fun handleEndringsvedtak(hendelse: Hendelse) {
        journalforingstatusRepository.upsert(
            Journalforingstatus(
                hendelseId = hendelse.id,
                journalpostId = null,
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

        hendelseIder.forEach {
            journalforingstatusRepository.upsert(
                Journalforingstatus(
                    hendelseId = it,
                    journalpostId = journalpostId,
                ),
            )
        }

        distribuer(nyesteHendelse, journalpostId)

        log.info(
            "Journalførte endringsvedtak for deltaker ${hendelser.first().deltaker.id}, " +
                "hendelser ${hendelser.map { it.id }.joinToString()}",
        )
    }

    private suspend fun distribuer(hendelse: Hendelse, journalpostId: String) {
        if (!hendelse.distribusjonskanal.skalDistribueresDigitalt()) {
            dokdistfordelingClient.distribuerJournalpost(journalpostId)
        }
    }

    private fun hendelseErBehandlet(hendelseId: UUID): Boolean {
        val journalforingstatus = journalforingstatusRepository.get(hendelseId)
        return journalforingstatus?.journalpostId != null
    }
}
