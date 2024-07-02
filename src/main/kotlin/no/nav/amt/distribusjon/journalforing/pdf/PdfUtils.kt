package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.utils.toTitleCase
import java.time.LocalDate

fun lagHovedvedtakPdfDto(
    deltaker: HendelseDeltaker,
    navBruker: NavBruker,
    utkast: Utkast,
    veileder: HendelseAnsvarlig.NavVeileder,
    vedtaksdato: LocalDate,
) = HovedvedtakPdfDto(
    deltaker = HovedvedtakPdfDto.DeltakerDto(
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        personident = deltaker.personident,
        innhold = utkast.innhold.toVisingstekst(),
        bakgrunnsinformasjon = if (utkast.bakgrunnsinformasjon.isNullOrEmpty()) {
            "—"
        } else {
            utkast.bakgrunnsinformasjon
        },
        deltakelsesmengdeTekst = if (skalViseDeltakelsesmengde(deltaker.deltakerliste.tiltak.type)) {
            utkast.deltakelsesprosent?.let {
                deltakelsesmengdeTekst(
                    deltakelsesprosent = it.toInt(),
                    dagerPerUke = utkast.dagerPerUke?.toInt(),
                )
            }
        } else {
            null
        },
        adresseDelesMedArrangor = adresseDelesMedArrangor(deltaker, navBruker),
    ),
    deltakerliste = HovedvedtakPdfDto.DeltakerlisteDto(
        navn = deltaker.deltakerliste.visningsnavn(),
        ledetekst = deltaker.deltakerliste.tiltak.ledetekst,
        arrangor = HovedvedtakPdfDto.ArrangorDto(
            navn = deltaker.deltakerliste.arrangor.visningsnavn(),
        ),
        forskriftskapittel = deltaker.deltakerliste.forskriftskapittel(),
    ),
    navVeileder = HovedvedtakPdfDto.NavVeilederDto(
        navn = veileder.navn,
        enhet = navBruker.navEnhet?.navn ?: "",
    ),
    vedtaksdato = vedtaksdato,
)

fun lagEndringsvedtakPdfDto(
    deltaker: HendelseDeltaker,
    navBruker: NavBruker,
    veileder: HendelseAnsvarlig.NavVeileder,
    hendelser: List<Hendelse>,
    vedtaksdato: LocalDate,
): EndringsvedtakPdfDto {
    val endringer = fjernEldreHendelserAvSammeType(hendelser).map { it.payload }

    return EndringsvedtakPdfDto(
        deltaker = EndringsvedtakPdfDto.DeltakerDto(
            fornavn = navBruker.fornavn,
            mellomnavn = navBruker.mellomnavn,
            etternavn = navBruker.etternavn,
            personident = deltaker.personident,
        ),
        deltakerliste = EndringsvedtakPdfDto.DeltakerlisteDto(
            navn = deltaker.deltakerliste.visningsnavn(),
            ledetekst = deltaker.deltakerliste.tiltak.ledetekst,
            arrangor = EndringsvedtakPdfDto.ArrangorDto(
                navn = deltaker.deltakerliste.arrangor.visningsnavn(),
            ),
            forskriftskapittel = deltaker.deltakerliste.forskriftskapittel(),
        ),
        endringer = endringer.map { tilEndringDto(it) },
        navVeileder = EndringsvedtakPdfDto.NavVeilederDto(
            navn = veileder.navn,
            enhet = navBruker.navEnhet?.navn ?: "",
        ),
        vedtaksdato = vedtaksdato,
        forsteVedtakFattet = deltaker.forsteVedtakFattet
            ?: throw IllegalStateException("Kan ikke journalføre endringsvedtak hvis opprinnelig vedtak ikke er fattet"),
    )
}

private fun fjernEldreHendelserAvSammeType(hendelser: List<Hendelse>): List<Hendelse> {
    return hendelser.sortedByDescending { it.opprettet }.distinctBy { it.payload.javaClass }
}

private fun skalViseDeltakelsesmengde(tiltakstype: HendelseDeltaker.Deltakerliste.Tiltak.Type): Boolean {
    return tiltakstype == HendelseDeltaker.Deltakerliste.Tiltak.Type.VASV ||
        tiltakstype == HendelseDeltaker.Deltakerliste.Tiltak.Type.ARBFORB
}

fun HendelseDeltaker.Deltakerliste.forskriftskapittel() = when (this.tiltak.type) {
    HendelseDeltaker.Deltakerliste.Tiltak.Type.INDOPPFAG -> 4
    HendelseDeltaker.Deltakerliste.Tiltak.Type.ARBFORB -> 13
    HendelseDeltaker.Deltakerliste.Tiltak.Type.AVKLARAG -> 2
    HendelseDeltaker.Deltakerliste.Tiltak.Type.VASV -> 14
    HendelseDeltaker.Deltakerliste.Tiltak.Type.ARBRRHDAG -> 12
    HendelseDeltaker.Deltakerliste.Tiltak.Type.DIGIOPPARB -> 4
    HendelseDeltaker.Deltakerliste.Tiltak.Type.JOBBK -> 4
    HendelseDeltaker.Deltakerliste.Tiltak.Type.GRUPPEAMO -> 7
    HendelseDeltaker.Deltakerliste.Tiltak.Type.GRUFAGYRKE -> 7
}

fun HendelseDeltaker.Deltakerliste.visningsnavn() = when (this.tiltak.type) {
    HendelseDeltaker.Deltakerliste.Tiltak.Type.DIGIOPPARB -> "Digital oppfølging hos ${this.arrangor.visningsnavn()}"
    HendelseDeltaker.Deltakerliste.Tiltak.Type.JOBBK -> "Jobbsøkerkurs hos ${arrangor.visningsnavn()}"
    HendelseDeltaker.Deltakerliste.Tiltak.Type.GRUPPEAMO -> if (this.erKurs) "Kurs: ${this.navn}" else this.navn
    HendelseDeltaker.Deltakerliste.Tiltak.Type.GRUFAGYRKE -> this.navn
    else -> "${this.tiltak.navn} hos ${arrangor.visningsnavn()}"
}

fun HendelseDeltaker.Deltakerliste.Arrangor.visningsnavn(): String {
    val visningsnavn = if (overordnetArrangor == null || overordnetArrangor.navn == "Ukjent Virksomhet") {
        navn
    } else {
        overordnetArrangor.navn
    }

    return toTitleCase(visningsnavn)
}

private fun adresseDelesMedArrangor(deltaker: HendelseDeltaker, navBruker: NavBruker): Boolean {
    return navBruker.adressebeskyttelse == null && deltaker.deltakerliste.deltakerAdresseDeles()
}

private fun List<Innhold>.toVisingstekst() = this.map { innhold ->
    "${innhold.tekst}${innhold.beskrivelse?.let { ": $it" } ?: ""}"
}

private fun tilEndringDto(hendelseType: HendelseType): EndringDto {
    return when (hendelseType) {
        is HendelseType.InnbyggerGodkjennUtkast,
        is HendelseType.NavGodkjennUtkast,
        is HendelseType.ReaktiverDeltakelse,
        is HendelseType.EndreSluttarsak,
        is HendelseType.EndreUtkast,
        is HendelseType.OpprettUtkast,
        is HendelseType.AvbrytUtkast,
        is HendelseType.DeltakerSistBesokt,
        -> throw IllegalArgumentException("Skal ikke journalføre $hendelseType som endringsvedtak")
        is HendelseType.AvsluttDeltakelse -> EndringDto.AvsluttDeltakelse(
            aarsak = hendelseType.aarsak.visningsnavn(),
            sluttdato = hendelseType.sluttdato,
        )
        is HendelseType.EndreDeltakelsesmengde -> EndringDto.EndreDeltakelsesmengde(
            deltakelsesprosent = hendelseType.deltakelsesprosent?.toInt(),
            dagerPerUkeTekst = dagerPerUkeTekst(hendelseType.dagerPerUke?.toInt()),
        )
        is HendelseType.EndreSluttdato -> EndringDto.EndreSluttdato(
            sluttdato = hendelseType.sluttdato,
        )
        is HendelseType.EndreStartdato -> {
            if (hendelseType.sluttdato != null) {
                EndringDto.EndreStartdatoOgVarighet(
                    startdato = hendelseType.startdato,
                    sluttdato = hendelseType.sluttdato,
                )
            } else {
                EndringDto.EndreStartdato(
                    startdato = hendelseType.startdato,
                )
            }
        }
        is HendelseType.ForlengDeltakelse -> EndringDto.ForlengDeltakelse(
            sluttdato = hendelseType.sluttdato,
            begrunnelseFraNav = hendelseType.begrunnelseFraNav,
            begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
        )
        is HendelseType.IkkeAktuell -> EndringDto.IkkeAktuell(
            aarsak = hendelseType.aarsak.visningsnavn(),
        )
        is HendelseType.EndreInnhold -> EndringDto.EndreInnhold(
            innhold = hendelseType.innhold.map { it.visningsnavn() },
        )
        is HendelseType.EndreBakgrunnsinformasjon -> EndringDto.EndreBakgrunnsinformasjon(
            bakgrunnsinformasjon = if (hendelseType.bakgrunnsinformasjon.isNullOrEmpty()) {
                "—"
            } else {
                hendelseType.bakgrunnsinformasjon
            },
        )
    }
}

private fun deltakelsesmengdeTekst(deltakelsesprosent: Int?, dagerPerUke: Int?): String {
    val dagerPerUkeTekst = dagerPerUkeTekst(dagerPerUke)?.lowercase()
    if (dagerPerUkeTekst != null) {
        return "${deltakelsesprosent ?: 100} % $dagerPerUkeTekst"
    }
    return "${deltakelsesprosent ?: 100} %"
}

private fun dagerPerUkeTekst(dagerPerUke: Int?): String? {
    if (dagerPerUke != null) {
        return if (dagerPerUke == 1) {
            "Fordelt på $dagerPerUke dag i uka"
        } else {
            "Fordelt på $dagerPerUke dager i uka"
        }
    }
    return null
}
