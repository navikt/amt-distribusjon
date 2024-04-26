package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.journalforing.person.model.Adresse
import no.nav.amt.distribusjon.journalforing.person.model.Kontaktadresse
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.journalforing.person.model.NavEnhet
import no.nav.amt.distribusjon.journalforing.person.model.Oppfolgingsperiode
import no.nav.amt.distribusjon.journalforing.person.model.Vegadresse
import java.time.LocalDateTime
import java.util.UUID

object Persondata {
    fun lagNavBruker(
        fornavn: String = "Navn",
        mellomnavn: String? = null,
        etternavn: String = "Navnersen",
        adresse: Adresse? = lagAdresse(),
        navEnhet: NavEnhet? = lagNavEnhet(),
        oppfolgingsperioder: List<Oppfolgingsperiode> = listOf(lagOppfolgingsperiode()),
    ) = NavBruker(fornavn, mellomnavn, etternavn, adresse, navEnhet, oppfolgingsperioder)

    private fun lagOppfolgingsperiode(
        id: UUID = UUID.randomUUID(),
        startdato: LocalDateTime = LocalDateTime.now().plusMonths(1),
        sluttdato: LocalDateTime? = null,
    ) = Oppfolgingsperiode(id, startdato, sluttdato)

    private fun lagNavEnhet(
        id: UUID = UUID.randomUUID(),
        enhetId: String = randomEnhetsnummer(),
        navn: String = "NAV Fyrstikkaleen",
    ) = NavEnhet(id, enhetId, navn)

    private fun lagAdresse(
        husnummer: String? = (1..100).random().toString(),
        husbokstav: String? = ('A'..'Z').random().toString(),
        adressenavn: String? = "Karl Johans Gate",
        tilleggsnavn: String? = null,
        postnummer: String = "0123",
        poststed: String = "Oslo",
    ) = Adresse(
        bostedsadresse = null,
        oppholdsadresse = null,
        kontaktadresse = Kontaktadresse(
            coAdressenavn = null,
            vegadresse = Vegadresse(
                husnummer,
                husbokstav,
                adressenavn,
                tilleggsnavn,
                postnummer,
                poststed,
            ),
            postboksadresse = null,
        ),
    )
}
