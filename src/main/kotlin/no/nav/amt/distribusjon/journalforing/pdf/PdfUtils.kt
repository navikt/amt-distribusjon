package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.utils.toTitleCase

fun lagHovedvedtakPdfDto(
    deltaker: HendelseDeltaker,
    navBruker: NavBruker,
    utkast: Utkast,
    veileder: HendelseAnsvarlig.NavVeileder,
) = HovedvedtakPdfDto(
    deltaker = HovedvedtakPdfDto.DeltakerDto(
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        personident = deltaker.personident,
        innhold = utkast.innhold.toVisingstekst(),
        bakgrunnsinformasjon = utkast.bakgrunnsinformasjon,
        deltakelsesmengde = utkast.deltakelsesprosent?.let {
            HovedvedtakPdfDto.DeltakelsesmengdeDto(
                deltakelsesprosent = it.toInt(),
                dagerPerUke = utkast.dagerPerUke?.toInt(),
            )
        },
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
)

fun lagEndringsvedtakPdfDto(
    deltaker: HendelseDeltaker,
    navBruker: NavBruker,
    veileder: HendelseAnsvarlig.NavVeileder,
    hendelser: List<Hendelse>,
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
    )
}

private fun fjernEldreHendelserAvSammeType(hendelser: List<Hendelse>): List<Hendelse> {
    return hendelser.sortedByDescending { it.opprettet }.distinctBy { it.payload.javaClass }
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

private fun List<Innhold>.toVisingstekst() = this.map { innhold ->
    "${innhold.tekst}${innhold.beskrivelse?.let { ": $it" } ?: ""}"
}

private fun tilEndringDto(hendelseType: HendelseType): EndringDto {
    return when (hendelseType) {
        is HendelseType.InnbyggerGodkjennUtkast,
        is HendelseType.NavGodkjennUtkast,
        is HendelseType.EndreSluttarsak,
        is HendelseType.EndreUtkast,
        is HendelseType.OpprettUtkast,
        is HendelseType.AvbrytUtkast,
        -> throw IllegalArgumentException("Skal ikke journalføre $hendelseType som endringsvedtak")
        is HendelseType.AvsluttDeltakelse -> EndringDto.AvsluttDeltakelse(
            aarsak = hendelseType.aarsak.visningsnavn(),
            sluttdato = hendelseType.sluttdato,
        )
        is HendelseType.EndreDeltakelsesmengde -> EndringDto.EndreDeltakelsesmengde(
            deltakelsesprosent = hendelseType.deltakelsesprosent,
            dagerPerUke = hendelseType.dagerPerUke,
        )
        is HendelseType.EndreSluttdato -> EndringDto.EndreSluttdato(
            sluttdato = hendelseType.sluttdato,
        )
        is HendelseType.EndreStartdato -> EndringDto.EndreStartdato(
            startdato = hendelseType.startdato,
            sluttdato = hendelseType.sluttdato,
        )
        is HendelseType.ForlengDeltakelse -> EndringDto.ForlengDeltakelse(
            sluttdato = hendelseType.sluttdato,
        )
        is HendelseType.IkkeAktuell -> EndringDto.IkkeAktuell(
            aarsak = hendelseType.aarsak.visningsnavn(),
        )
        is HendelseType.EndreInnhold -> EndringDto.EndreInnhold(
            innhold = hendelseType.innhold.map { it.tekst },
        )
        is HendelseType.EndreBakgrunnsinformasjon -> EndringDto.EndreBakgrunnsinformasjon(
            bakgrunnsinformasjon = hendelseType.bakgrunnsinformasjon,
        )
    }
}
