package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.hendelse.model.ArenaTiltakTypeKode
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
    begrunnelseFraNav: String?,
) = HovedvedtakPdfDto(
    deltaker = HovedvedtakPdfDto.DeltakerDto(
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        personident = deltaker.personident,
        innhold = utkast.innhold.toVisingstekst(),
        bakgrunnsinformasjon = utkast.bakgrunnsinformasjon,
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
    begrunnelseFraNav = begrunnelseFraNav,
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

private fun fjernEldreHendelserAvSammeType(hendelser: List<Hendelse>): List<Hendelse> = hendelser
    .sortedByDescending { it.opprettet }
    .distinctBy { it.payload.javaClass }

private fun skalViseDeltakelsesmengde(tiltakstype: ArenaTiltakTypeKode): Boolean = tiltakstype == ArenaTiltakTypeKode.VASV ||
    tiltakstype == ArenaTiltakTypeKode.ARBFORB

fun HendelseDeltaker.Deltakerliste.forskriftskapittel() = when (this.tiltak.type) {
    ArenaTiltakTypeKode.INDOPPFAG -> 4
    ArenaTiltakTypeKode.ARBFORB -> 13
    ArenaTiltakTypeKode.AVKLARAG -> 2
    ArenaTiltakTypeKode.VASV -> 14
    ArenaTiltakTypeKode.ARBRRHDAG -> 12
    ArenaTiltakTypeKode.DIGIOPPARB -> 4
    ArenaTiltakTypeKode.JOBBK -> 4
    ArenaTiltakTypeKode.GRUPPEAMO -> 7
    ArenaTiltakTypeKode.GRUFAGYRKE -> 7
}

fun HendelseDeltaker.Deltakerliste.visningsnavn() = when (this.tiltak.type) {
    ArenaTiltakTypeKode.DIGIOPPARB -> "Digital oppfølging hos ${this.arrangor.visningsnavn()}"
    ArenaTiltakTypeKode.JOBBK -> "Jobbsøkerkurs hos ${arrangor.visningsnavn()}"
    ArenaTiltakTypeKode.GRUPPEAMO -> if (this.erKurs) "Kurs: ${this.navn}" else this.navn
    ArenaTiltakTypeKode.GRUFAGYRKE -> this.navn
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

private fun adresseDelesMedArrangor(deltaker: HendelseDeltaker, navBruker: NavBruker): Boolean =
    navBruker.adressebeskyttelse == null && deltaker.deltakerliste.deltakerAdresseDeles()

private fun List<Innhold>.toVisingstekst() = this.map { innhold ->
    "${innhold.tekst}${innhold.beskrivelse?.let { ": $it" } ?: ""}"
}

private fun tilEndringDto(hendelseType: HendelseType): EndringDto = when (hendelseType) {
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
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
    )

    is HendelseType.EndreDeltakelsesmengde -> EndringDto.EndreDeltakelsesmengde(
        deltakelsesprosent = hendelseType.deltakelsesprosent?.toInt(),
        dagerPerUkeTekst = dagerPerUkeTekst(hendelseType.dagerPerUke?.toInt()),
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
    )

    is HendelseType.EndreSluttdato -> EndringDto.EndreSluttdato(
        sluttdato = hendelseType.sluttdato,
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
    )

    is HendelseType.EndreStartdato -> {
        if (hendelseType.sluttdato != null) {
            EndringDto.EndreStartdatoOgVarighet(
                startdato = hendelseType.startdato,
                sluttdato = hendelseType.sluttdato,
                begrunnelseFraNav = hendelseType.begrunnelseFraNav,
                begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
            )
        } else {
            EndringDto.EndreStartdato(
                startdato = hendelseType.startdato,
                begrunnelseFraNav = hendelseType.begrunnelseFraNav,
                begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
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
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        begrunnelseFraArrangor = hendelseType.begrunnelseFraArrangor,
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

    is HendelseType.LeggTilOppstartsdato -> EndringDto.LeggTilOppstartsdato(
        startdatoFraArrangor = hendelseType.startdato,
        sluttdatoFraArrangor = hendelseType.sluttdato,
    )
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
            "fordelt på $dagerPerUke dag i uka"
        } else {
            "fordelt på $dagerPerUke dager i uka"
        }
    }
    return null
}
