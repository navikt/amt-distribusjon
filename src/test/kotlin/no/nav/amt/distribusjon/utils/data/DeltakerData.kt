package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.amtdeltaker.AmtDeltakerResponse
import no.nav.amt.distribusjon.amtdeltaker.Deltakerliste
import no.nav.amt.distribusjon.amtdeltaker.Tiltakstype
import no.nav.amt.distribusjon.hendelse.model.ArenaTiltakTypeKode
import no.nav.amt.distribusjon.hendelse.model.Tiltakskode
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object DeltakerData {
    fun lagDeltaker() = AmtDeltakerResponse(
        id = UUID.randomUUID(),
        navBruker = Persondata.lagNavBruker().toNavBruker(),
        deltakerliste = lagDeltakerListe(),
        startdato = null,
        sluttdato = null,
        dagerPerUke = null,
        deltakelsesprosent = null,
        bakgrunnsinformasjon = null,
        deltakelsesinnhold = "",
        status = "",
        vedtaksinformasjon = null,
        sistEndret = LocalDateTime.now(),
    )

    fun lagDeltakerListe() = Deltakerliste(
        id = UUID.randomUUID(),
        tiltakstype = lagTiltakstype(),
        navn = "deltakerliste navn",
        status = Deltakerliste.Status.GJENNOMFORES,
        startDato = LocalDate.now(),
        sluttDato = null,
        oppstart = null,
        arrangor = "arrangor",
    )

    fun lagTiltakstype() = Tiltakstype(
        id = UUID.randomUUID(),
        navn = "Tiltaksnavn",
        tiltakskode = Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
        arenaKode = ArenaTiltakTypeKode.ARBFORB,
        innsatsgrupper = setOf("SITUASJONSBESTEMT_INNSATS"),
        innhold = null,
    )
}

fun NavBruker.toNavBruker() = no.nav.amt.distribusjon.amtdeltaker.NavBruker(
    personId = UUID.randomUUID(),
    personident = "1234567888",
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    navVeilederId = UUID.randomUUID(),
    navEnhetId = UUID.randomUUID(),
    telefon = null,
    epost = null,
    erSkjermet = false,
    adresse = null,
    adressebeskyttelse = null,
    oppfolgingsperioder = oppfolgingsperioder,
    innsatsgruppe = null,
)
