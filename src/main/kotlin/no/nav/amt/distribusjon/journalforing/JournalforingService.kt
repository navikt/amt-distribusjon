package no.nav.amt.distribusjon.journalforing

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.pdf.lagHovedvedtakPdfDto
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import org.slf4j.LoggerFactory

class JournalforingService(
    private val amtPersonClient: AmtPersonClient,
    private val pdfgenClient: PdfgenClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun handleHendelse(hendelse: Hendelse) {
        when (hendelse.payload) {
            is HendelseType.InnbyggerGodkjennUtkast -> journalforHovedvedtak(hendelse.deltaker, hendelse.payload.utkast, hendelse.ansvarlig)
            is HendelseType.NavGodkjennUtkast -> journalforHovedvedtak(hendelse.deltaker, hendelse.payload.utkast, hendelse.ansvarlig)
            is HendelseType.AvsluttDeltakelse,
            is HendelseType.EndreBakgrunnsinformasjon,
            is HendelseType.EndreDeltakelsesmengde,
            is HendelseType.EndreSluttdato,
            is HendelseType.EndreStartdato,
            is HendelseType.ForlengDeltakelse,
            is HendelseType.IkkeAktuell,
            -> journalforEndringsvedtak(hendelse)

            is HendelseType.EndreSluttarsak,
            is HendelseType.EndreInnhold,
            is HendelseType.EndreUtkast,
            is HendelseType.OpprettUtkast,
            is HendelseType.AvbrytUtkast,
            -> {
            }
        }
    }

    private suspend fun journalforHovedvedtak(
        deltaker: HendelseDeltaker,
        utkast: Utkast,
        ansvarlig: HendelseAnsvarlig,
    ) {
        val veileder = when (ansvarlig) {
            is HendelseAnsvarlig.NavVeileder -> ansvarlig
        }
        val navBruker = amtPersonClient.hentNavBruker(deltaker.personident)
        val pdf = pdfgenClient.hovedvedtak(lagHovedvedtakPdfDto(deltaker, navBruker, utkast, veileder))

        log.info("Journalførte hovedvedtak for deltaker ${deltaker.id}")
    }

    private fun journalforEndringsvedtak(hendelse: Hendelse) {
        log.info("Journalførte endringsvedtak for deltaker ${hendelse.deltaker.id}")
    }
}
