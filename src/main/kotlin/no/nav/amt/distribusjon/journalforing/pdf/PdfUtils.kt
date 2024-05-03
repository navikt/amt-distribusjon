package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.journalforing.person.model.toAdresselinjer
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
        adresselinjer = navBruker.adresse?.toAdresselinjer() ?: emptyList(),
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
    endringer: List<HendelseType>,
) = EndringsvedtakPdfDto(
    deltaker = EndringsvedtakPdfDto.DeltakerDto(
        fornavn = navBruker.fornavn,
        mellomnavn = navBruker.mellomnavn,
        etternavn = navBruker.etternavn,
        adresselinjer = navBruker.adresse?.toAdresselinjer() ?: emptyList(),
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

private fun tilEndringDto(hendelseType: HendelseType): EndringsvedtakPdfDto.EndringDto {
    return when (hendelseType) {
        is HendelseType.InnbyggerGodkjennUtkast,
        is HendelseType.NavGodkjennUtkast,
        is HendelseType.EndreSluttarsak,
        is HendelseType.EndreInnhold,
        is HendelseType.EndreBakgrunnsinformasjon,
        is HendelseType.EndreUtkast,
        is HendelseType.OpprettUtkast,
        is HendelseType.AvbrytUtkast,
        -> throw IllegalArgumentException("Skal ikke journalføre $hendelseType som endringsvedtak")
        is HendelseType.AvsluttDeltakelse -> EndringsvedtakPdfDto.EndringDto(
            "Avslutt deltakelse",
            hendelseType,
        )
        is HendelseType.EndreDeltakelsesmengde -> EndringsvedtakPdfDto.EndringDto(
            "Deltakelsesmengde",
            hendelseType,
        )
        is HendelseType.EndreSluttdato -> EndringsvedtakPdfDto.EndringDto(
            "Endre sluttdato",
            hendelseType,
        )
        is HendelseType.EndreStartdato -> EndringsvedtakPdfDto.EndringDto(
            "Endre startdato",
            hendelseType,
        )
        is HendelseType.ForlengDeltakelse -> EndringsvedtakPdfDto.EndringDto(
            "Forlengelse",
            hendelseType,
        )
        is HendelseType.IkkeAktuell -> EndringsvedtakPdfDto.EndringDto(
            "Ikke aktuell",
            hendelseType,
        )
    }
}
