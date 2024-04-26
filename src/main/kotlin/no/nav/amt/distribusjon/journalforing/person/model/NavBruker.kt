package no.nav.amt.distribusjon.journalforing.person.model

import java.time.LocalDateTime
import java.util.UUID

data class NavBruker(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val adresse: Adresse?,
    val navEnhet: NavEnhet?,
    val oppfolgingsperioder: List<Oppfolgingsperiode>,
)

data class Oppfolgingsperiode(
    val id: UUID,
    val startdato: LocalDateTime,
    val sluttdato: LocalDateTime?,
)
