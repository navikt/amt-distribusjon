package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.journalforing.person.model.NavEnhet
import no.nav.amt.distribusjon.journalforing.person.model.Oppfolgingsperiode
import java.time.LocalDateTime
import java.util.UUID

object Persondata {
    fun lagNavBruker(
        fornavn: String = "Navn",
        mellomnavn: String? = null,
        etternavn: String = "Navnersen",
        navEnhet: NavEnhet? = lagNavEnhet(),
        oppfolgingsperioder: List<Oppfolgingsperiode> = listOf(lagOppfolgingsperiode()),
    ) = NavBruker(fornavn, mellomnavn, etternavn, navEnhet, oppfolgingsperioder)

    fun lagOppfolgingsperiode(
        id: UUID = UUID.randomUUID(),
        startdato: LocalDateTime = LocalDateTime.now().minusMonths(1),
        sluttdato: LocalDateTime? = null,
    ) = Oppfolgingsperiode(id, startdato, sluttdato)

    private fun lagNavEnhet(
        id: UUID = UUID.randomUUID(),
        enhetId: String = randomEnhetsnummer(),
        navn: String = "NAV Fyrstikkaleen",
    ) = NavEnhet(id, enhetId, navn)
}
