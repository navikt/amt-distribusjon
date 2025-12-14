package no.nav.amt.distribusjon.journalforing.person

import no.nav.amt.distribusjon.exchangeWithLogging
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AmtPersonClient(
    private val personServiceHttpClient: RestClient,
) {
    fun hentNavBruker(personident: String): NavBruker = personServiceHttpClient
        .post()
        .uri("/api/nav-bruker")
        .body(NavBrukerRequest(personident))
        .exchangeWithLogging("Kunne ikke hente Nav-bruker fra amt-person-service")
}

data class NavBrukerRequest(
    val personident: String,
)
