package no.nav.amt.distribusjon.amtdeltaker

import no.nav.amt.distribusjon.exchangeWithLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class AmtDeltakerClient(
    private val deltakerHttpClient: RestClient,
) {
    fun getDeltaker(deltakerId: UUID): AmtDeltakerResponse = deltakerHttpClient
        .get()
        .uri("/deltaker/$deltakerId")
        .exchangeWithLogging("Kunne ikke hente deltaker fra amt-deltaker")
}
