package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.hendelse.model.Aarsak
import no.nav.amt.distribusjon.hendelse.model.ArenaTiltakTypeKode
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.distribusjon.hendelse.model.toTiltakskode
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.utils.formatDate
import no.nav.amt.distribusjon.utils.toTitleCase
import no.nav.amt.lib.models.arrangor.melding.EndringAarsak
import no.nav.amt.lib.models.arrangor.melding.Forslag
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
        innhold = utkast.innhold?.toVisingstekst() ?: emptyList(),
        innholdBeskrivelse = utkast.innhold?.firstOrNull { it.innholdskode == "annet" }?.beskrivelse,
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
        tiltakskode = deltaker.deltakerliste.tiltak.type.toTiltakskode(),
        ledetekst = deltaker.deltakerliste.tiltak.ledetekst,
        arrangor = HovedvedtakPdfDto.ArrangorDto(
            navn = deltaker.deltakerliste.arrangor.visningsnavn(),
        ),
        forskriftskapittel = deltaker.deltakerliste.forskriftskapittel(),
    ),
    avsender = HovedvedtakPdfDto.AvsenderDto(
        navn = veileder.navn,
        enhet = navBruker.navEnhet?.navn ?: "NAV",
    ),
    vedtaksdato = vedtaksdato,
    begrunnelseFraNav = begrunnelseFraNav,
)

fun lagEndringsvedtakPdfDto(
    deltaker: HendelseDeltaker,
    navBruker: NavBruker,
    ansvarlig: HendelseAnsvarlig,
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
        endringer = endringer.map { tilEndringDto(it, deltaker.deltakerliste.tiltak.type) },
        avsender = EndringsvedtakPdfDto.AvsenderDto(
            navn = ansvarlig.getAvsendernavn(),
            enhet = navBruker.navEnhet?.navn ?: "NAV",
        ),
        vedtaksdato = vedtaksdato,
        forsteVedtakFattet = deltaker.forsteVedtakFattet
            ?: throw IllegalStateException("Kan ikke journalføre endringsvedtak hvis opprinnelig vedtak ikke er fattet"),
    )
}

private fun HendelseAnsvarlig.getAvsendernavn() = when (this) {
    is HendelseAnsvarlig.NavVeileder -> navn
    is HendelseAnsvarlig.Arrangor -> null
    is HendelseAnsvarlig.Deltaker -> throw IllegalArgumentException("Kan ikke journalføre endringsvedtak fra deltaker")
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
    ArenaTiltakTypeKode.VASV -> "Varig tilrettelagt arbeid hos ${this.arrangor.visningsnavn()}"
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

private fun tilEndringDto(hendelseType: HendelseType, tiltakskode: ArenaTiltakTypeKode): EndringDto = when (hendelseType) {
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
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        forslagFraArrangor = hendelseType.endringFraForslag?.let { endringFraForslagToForslagDto(it, hendelseType.begrunnelseFraArrangor) },
        tittel = "Ny sluttdato er ${formatDate(hendelseType.sluttdato)}",
    )

    is HendelseType.EndreDeltakelsesmengde -> EndringDto.EndreDeltakelsesmengde(
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        forslagFraArrangor = hendelseType.endringFraForslag?.let { endringFraForslagToForslagDto(it, hendelseType.begrunnelseFraArrangor) },
        tittel = "Deltakelsen er endret til ${deltakelsesmengdeTekst(
            deltakelsesprosent = hendelseType.deltakelsesprosent?.toInt(),
            dagerPerUke = hendelseType.dagerPerUke?.toInt(),
        )}",
        gyldigFra = hendelseType.gyldigFra,
    )

    is HendelseType.EndreSluttdato -> EndringDto.EndreSluttdato(
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        forslagFraArrangor = hendelseType.endringFraForslag?.let { endringFraForslagToForslagDto(it, hendelseType.begrunnelseFraArrangor) },
        tittel = "Ny sluttdato er ${formatDate(hendelseType.sluttdato)}",
    )

    is HendelseType.EndreStartdato -> {
        val tittel = if (hendelseType.startdato != null) {
            "Oppstartsdato er endret til ${formatDate(hendelseType.startdato)}"
        } else {
            "Oppstartsdato er fjernet"
        }
        if (hendelseType.sluttdato != null) {
            EndringDto.EndreStartdatoOgVarighet(
                sluttdato = hendelseType.sluttdato,
                begrunnelseFraNav = hendelseType.begrunnelseFraNav,
                forslagFraArrangor = hendelseType.endringFraForslag?.let {
                    endringFraForslagToForslagDto(
                        it,
                        hendelseType.begrunnelseFraArrangor,
                    )
                },
                tittel = tittel,
            )
        } else {
            EndringDto.EndreStartdato(
                begrunnelseFraNav = hendelseType.begrunnelseFraNav,
                forslagFraArrangor = hendelseType.endringFraForslag?.let {
                    endringFraForslagToForslagDto(
                        it,
                        hendelseType.begrunnelseFraArrangor,
                    )
                },
                tittel = tittel,
            )
        }
    }

    is HendelseType.ForlengDeltakelse -> EndringDto.ForlengDeltakelse(
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        forslagFraArrangor = hendelseType.endringFraForslag?.let { endringFraForslagToForslagDto(it, hendelseType.begrunnelseFraArrangor) },
        tittel = "Deltakelsen er forlenget til ${formatDate(hendelseType.sluttdato)}",
    )

    is HendelseType.IkkeAktuell -> EndringDto.IkkeAktuell(
        aarsak = hendelseType.aarsak.visningsnavn(),
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        forslagFraArrangor = hendelseType.endringFraForslag?.let { endringFraForslagToForslagDto(it, hendelseType.begrunnelseFraArrangor) },
    )

    is HendelseType.EndreInnhold -> EndringDto.EndreInnhold(
        innhold = hendelseType.innhold.map { it.visningsnavn() },
        innholdBeskrivelse = if (tiltakskode == ArenaTiltakTypeKode.VASV) {
            hendelseType.innhold.firstOrNull { it.innholdskode == "annet" }?.beskrivelse
        } else {
            null
        },
    )

    is HendelseType.EndreBakgrunnsinformasjon -> EndringDto.EndreBakgrunnsinformasjon(
        bakgrunnsinformasjon = if (hendelseType.bakgrunnsinformasjon.isNullOrEmpty()) {
            "—"
        } else {
            hendelseType.bakgrunnsinformasjon
        },
    )

    is HendelseType.LeggTilOppstartsdato -> EndringDto.LeggTilOppstartsdato(
        sluttdatoFraArrangor = hendelseType.sluttdato,
        tittel = "Oppstartsdato er ${formatDate(hendelseType.startdato)}",
    )

    is HendelseType.FjernOppstartsdato -> EndringDto.FjernOppstartsdato(
        begrunnelseFraNav = hendelseType.begrunnelseFraNav,
        forslagFraArrangor = hendelseType.endringFraForslag?.let { endringFraForslagToForslagDto(it, hendelseType.begrunnelseFraArrangor) },
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

private fun endringFraForslagToForslagDto(endring: Forslag.Endring, begrunnelseFraArrangor: String?): ForslagDto = when (endring) {
    is Forslag.ForlengDeltakelse -> ForslagDto.ForlengDeltakelse(
        sluttdato = endring.sluttdato,
        begrunnelseFraArrangor = begrunnelseFraArrangor,
    )
    is Forslag.AvsluttDeltakelse -> ForslagDto.AvsluttDeltakelse(
        aarsak = endring.aarsak.toAarsak().visningsnavn(),
        sluttdato = endring.sluttdato,
        harDeltatt = endring.harDeltatt?.let { if (it) "Ja" else "Nei" },
        begrunnelseFraArrangor = begrunnelseFraArrangor,
    )
    is Forslag.Deltakelsesmengde -> ForslagDto.EndreDeltakelsesmengde(
        deltakelsesmengdeTekst = deltakelsesmengdeTekst(
            deltakelsesprosent = endring.deltakelsesprosent,
            dagerPerUke = endring.dagerPerUke,
        ),
        begrunnelseFraArrangor = begrunnelseFraArrangor,
    )
    is Forslag.IkkeAktuell -> ForslagDto.IkkeAktuell(
        aarsak = endring.aarsak.toAarsak().visningsnavn(),
        begrunnelseFraArrangor = begrunnelseFraArrangor,
    )
    is Forslag.Sluttdato -> ForslagDto.EndreSluttdato(
        sluttdato = endring.sluttdato,
        begrunnelseFraArrangor = begrunnelseFraArrangor,
    )
    is Forslag.Startdato -> {
        if (endring.sluttdato != null) {
            ForslagDto.EndreStartdatoOgVarighet(
                startdato = endring.startdato,
                sluttdato = endring.sluttdato!!,
                begrunnelseFraArrangor = begrunnelseFraArrangor,
            )
        } else {
            ForslagDto.EndreStartdato(
                startdato = endring.startdato,
                begrunnelseFraArrangor = begrunnelseFraArrangor,
            )
        }
    }
    is Forslag.FjernOppstartsdato -> ForslagDto.FjernOppstartsdato(
        begrunnelseFraArrangor = begrunnelseFraArrangor,
    )
    is Forslag.Sluttarsak -> throw IllegalArgumentException("Skal ikke opprette endringsvedtak ved endring av sluttårsak")
}

private fun EndringAarsak.toAarsak(): Aarsak = when (this) {
    is EndringAarsak.FattJobb -> Aarsak(Aarsak.Type.FATT_JOBB)
    is EndringAarsak.Annet -> Aarsak(Aarsak.Type.ANNET, beskrivelse)
    is EndringAarsak.IkkeMott -> Aarsak(Aarsak.Type.IKKE_MOTT)
    is EndringAarsak.Syk -> Aarsak(Aarsak.Type.SYK)
    is EndringAarsak.TrengerAnnenStotte -> Aarsak(Aarsak.Type.TRENGER_ANNEN_STOTTE)
    is EndringAarsak.Utdanning -> Aarsak(Aarsak.Type.UTDANNING)
}
