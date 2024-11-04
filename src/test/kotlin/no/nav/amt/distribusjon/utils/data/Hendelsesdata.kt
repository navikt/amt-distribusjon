package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Aarsak
import no.nav.amt.distribusjon.hendelse.model.ArenaTiltakTypeKode
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseDto
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.hendelse.model.Tiltak
import no.nav.amt.distribusjon.hendelse.model.Utkast
import no.nav.amt.lib.models.arrangor.melding.EndringAarsak
import no.nav.amt.lib.models.arrangor.melding.Forslag
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

object Hendelsesdata {
    fun hendelseDto(
        payload: HendelseType,
        id: UUID = UUID.randomUUID(),
        deltaker: HendelseDeltaker = deltaker(),
        ansvarlig: HendelseAnsvarlig = ansvarligNavVeileder(),
        opprettet: LocalDateTime = LocalDateTime.now(),
    ) = HendelseDto(
        id,
        opprettet,
        deltaker,
        ansvarlig,
        payload,
    )

    fun hendelse(
        payload: HendelseType,
        id: UUID = UUID.randomUUID(),
        deltaker: HendelseDeltaker = deltaker(),
        ansvarlig: HendelseAnsvarlig = ansvarligNavVeileder(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        distribusjonskanal: Distribusjonskanal = Distribusjonskanal.DITT_NAV,
        manuellOppfolging: Boolean = false,
    ) = hendelseDto(
        payload,
        id,
        deltaker,
        ansvarlig,
        opprettet,
    ).toModel(distribusjonskanal, manuellOppfolging)

    fun ansvarligNavVeileder(
        id: UUID = UUID.randomUUID(),
        navn: String = "Veilder Veildersen",
        navIdent: String = randomNavIdent(),
        enhet: HendelseAnsvarlig.NavVeileder.Enhet = ansvarligNavEnhet(),
    ) = HendelseAnsvarlig.NavVeileder(id, navn, navIdent, enhet)

    fun ansvarligNavEnhet(id: UUID = UUID.randomUUID(), enhetsnummer: String = randomEnhetsnummer()) =
        HendelseAnsvarlig.NavVeileder.Enhet(id, enhetsnummer)

    fun deltaker(
        id: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        deltakerliste: HendelseDeltaker.Deltakerliste = deltakerliste(),
        forsteVedtakFattet: LocalDate? = LocalDate.now().minusDays(3),
    ) = HendelseDeltaker(
        id,
        personident,
        deltakerliste,
        forsteVedtakFattet,
    )

    fun deltakerliste(
        id: UUID = UUID.randomUUID(),
        navn: String = "Deltakerlistenavn",
        arrangor: HendelseDeltaker.Deltakerliste.Arrangor = arrangor(),
        tiltak: Tiltak = tiltak(),
    ) = HendelseDeltaker.Deltakerliste(id, navn, arrangor, tiltak)

    fun arrangor(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = randomOrgnr(),
        navn: String = "Arrangornavn",
        overordnetArrangor: HendelseDeltaker.Deltakerliste.Arrangor? = null,
    ) = HendelseDeltaker.Deltakerliste.Arrangor(id, organisasjonsnummer, navn, overordnetArrangor)

    fun tiltak(
        navn: String = "Tiltaksnavn",
        type: ArenaTiltakTypeKode = ArenaTiltakTypeKode.ARBFORB,
        ledetekst: String = "Beskrivelse av hva tiltaket går ut på",
    ) = Tiltak(navn, type, ledetekst)
}

object HendelseTypeData {
    fun opprettUtkast(utkast: Utkast = utkast()) = HendelseType.OpprettUtkast(utkast)

    fun avbrytUtkast(utkast: Utkast = utkast()) = HendelseType.AvbrytUtkast(utkast)

    fun innbyggerGodkjennUtkast(utkast: Utkast = utkast()) = HendelseType.InnbyggerGodkjennUtkast(utkast)

    fun navGodkjennUtkast(utkast: Utkast = utkast()) = HendelseType.NavGodkjennUtkast(utkast)

    fun endreInnhold(innhold: List<Innhold> = listOf(innhold())) = HendelseType.EndreInnhold(innhold)

    fun endreDeltakelsesmengde(
        deltakelsesprosent: Float? = 99F,
        dagerPerUke: Float? = 5F,
        gyldigFra: LocalDate = LocalDate.now(),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.Deltakelsesmengde(50, 3, LocalDate.now()),
    ) = HendelseType.EndreDeltakelsesmengde(
        deltakelsesprosent,
        dagerPerUke,
        gyldigFra,
        begrunnelseFraNav,
        begrunnelseFraArrangor,
        endringFraForslag,
    )

    fun endreStartdato(
        startdato: LocalDate? = LocalDate.now().plusDays(7),
        sluttdato: LocalDate? = null,
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.Startdato(LocalDate.now().plusDays(5), null),
    ) = HendelseType.EndreStartdato(startdato, sluttdato, begrunnelseFraNav, begrunnelseFraArrangor, endringFraForslag)

    fun endreSluttdato(
        sluttdato: LocalDate = LocalDate.now().plusDays(7),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.Sluttdato(sluttdato),
    ) = HendelseType.EndreSluttdato(sluttdato, begrunnelseFraNav, begrunnelseFraArrangor, endringFraForslag)

    fun forlengDeltakelse(
        sluttdato: LocalDate = LocalDate.now().plusMonths(2),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.ForlengDeltakelse(sluttdato),
    ) = HendelseType.ForlengDeltakelse(sluttdato, begrunnelseFraNav, begrunnelseFraArrangor, endringFraForslag)

    fun ikkeAktuell(
        aarsak: Aarsak = Aarsak(Aarsak.Type.FATT_JOBB, null),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.IkkeAktuell(EndringAarsak.FattJobb),
    ) = HendelseType.IkkeAktuell(aarsak, begrunnelseFraNav, begrunnelseFraArrangor, endringFraForslag)

    fun avsluttDeltakelse(
        aarsak: Aarsak = Aarsak(Aarsak.Type.FATT_JOBB, null),
        sluttdato: LocalDate = LocalDate.now().plusDays(7),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.AvsluttDeltakelse(sluttdato, EndringAarsak.FattJobb, true),
    ) = HendelseType.AvsluttDeltakelse(aarsak, sluttdato, begrunnelseFraNav, begrunnelseFraArrangor, endringFraForslag)

    fun endreSluttarsak(
        aarsak: Aarsak = Aarsak(Aarsak.Type.ANNET, "Noe annet"),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
        endringFraForslag: Forslag.Endring? = Forslag.Sluttarsak(EndringAarsak.Annet("annet")),
    ) = HendelseType.EndreSluttarsak(aarsak, begrunnelseFraNav, begrunnelseFraArrangor, endringFraForslag)

    fun sistBesokt(sistBesokt: ZonedDateTime = ZonedDateTime.now()) = HendelseType.DeltakerSistBesokt(sistBesokt)

    fun utkast(
        startdato: LocalDate? = null,
        sluttdato: LocalDate? = null,
        dagerPerUke: Float? = 4F,
        deltakelsesprosent: Float = 80F,
        bakgrunnsinformasjon: String = "Bakgrunn for deltakelse på tiltak",
        innhold: List<Innhold> = listOf(innhold(), innhold(), innhold()),
    ) = Utkast(
        startdato,
        sluttdato,
        dagerPerUke,
        deltakelsesprosent,
        bakgrunnsinformasjon,
        innhold,
    )

    fun innhold() = Innhold(
        "Innholdstekst",
        "Innholdskode",
        "Beskrivelse av annet innhold",
    )
}
