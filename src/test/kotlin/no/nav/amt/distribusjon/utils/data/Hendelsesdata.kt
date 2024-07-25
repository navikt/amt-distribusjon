package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Aarsak
import no.nav.amt.distribusjon.hendelse.model.HendelseAnsvarlig
import no.nav.amt.distribusjon.hendelse.model.HendelseDeltaker
import no.nav.amt.distribusjon.hendelse.model.HendelseDto
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.hendelse.model.Innhold
import no.nav.amt.distribusjon.hendelse.model.Utkast
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
        tiltak: HendelseDeltaker.Deltakerliste.Tiltak = tiltak(),
    ) = HendelseDeltaker.Deltakerliste(id, navn, arrangor, tiltak)

    fun arrangor(
        id: UUID = UUID.randomUUID(),
        organisasjonsnummer: String = randomOrgnr(),
        navn: String = "Arrangornavn",
        overordnetArrangor: HendelseDeltaker.Deltakerliste.Arrangor? = null,
    ) = HendelseDeltaker.Deltakerliste.Arrangor(id, organisasjonsnummer, navn, overordnetArrangor)

    fun tiltak(
        navn: String = "Tiltaksnavn",
        type: HendelseDeltaker.Deltakerliste.Tiltak.Type = HendelseDeltaker.Deltakerliste.Tiltak.Type.ARBFORB,
        ledetekst: String = "Beskrivelse av hva tiltaket går ut på",
    ) = HendelseDeltaker.Deltakerliste.Tiltak(navn, type, ledetekst)
}

object HendelseTypeData {
    fun opprettUtkast(utkast: Utkast = utkast()) = HendelseType.OpprettUtkast(utkast)

    fun avbrytUtkast(utkast: Utkast = utkast()) = HendelseType.AvbrytUtkast(utkast)

    fun innbyggerGodkjennUtkast(utkast: Utkast = utkast()) = HendelseType.InnbyggerGodkjennUtkast(utkast)

    fun navGodkjennUtkast(utkast: Utkast = utkast()) = HendelseType.NavGodkjennUtkast(utkast)

    fun endreBakgrunnsinformasjon(bakgrunnsinformasjon: String = "Ny bakgrunn") =
        HendelseType.EndreBakgrunnsinformasjon(bakgrunnsinformasjon)

    fun endreInnhold(innhold: List<Innhold> = listOf(innhold())) = HendelseType.EndreInnhold(innhold)

    fun endreDeltakelsesmengde(
        deltakelsesprosent: Float? = 99F,
        dagerPerUke: Float? = 5F,
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
    ) = HendelseType.EndreDeltakelsesmengde(deltakelsesprosent, dagerPerUke, begrunnelseFraNav, begrunnelseFraArrangor)

    fun endreStartdato(startdato: LocalDate? = LocalDate.now().plusDays(7), sluttdato: LocalDate? = null) =
        HendelseType.EndreStartdato(startdato, sluttdato)

    fun endreSluttdato(
        sluttdato: LocalDate = LocalDate.now().plusDays(7),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
    ) = HendelseType.EndreSluttdato(sluttdato, begrunnelseFraNav, begrunnelseFraArrangor)

    fun forlengDeltakelse(
        sluttdato: LocalDate = LocalDate.now().plusMonths(2),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
    ) = HendelseType.ForlengDeltakelse(sluttdato, begrunnelseFraNav, begrunnelseFraArrangor)

    fun ikkeAktuell(
        aarsak: Aarsak = Aarsak(Aarsak.Type.FATT_JOBB, null),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
    ) = HendelseType.IkkeAktuell(aarsak, begrunnelseFraNav, begrunnelseFraArrangor)

    fun avsluttDeltakelse(
        aarsak: Aarsak = Aarsak(Aarsak.Type.FATT_JOBB, null),
        sluttdato: LocalDate = LocalDate.now().plusDays(7),
        begrunnelseFraNav: String? = "begrunnelse",
        begrunnelseFraArrangor: String? = "Begrunnelse fra arrangør",
    ) = HendelseType.AvsluttDeltakelse(aarsak, sluttdato, begrunnelseFraNav, begrunnelseFraArrangor)

    fun endreSluttarsak(aarsak: Aarsak = Aarsak(Aarsak.Type.ANNET, "Noe annet")) = HendelseType.EndreSluttarsak(aarsak)

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
