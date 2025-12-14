package no.nav.amt.distribusjon.distribusjonskanal

import no.nav.amt.distribusjon.AppConstants.NAV_CALL_ID_HEADER_KEY
import no.nav.amt.distribusjon.config.CacheConfig.Companion.DISTRIBUSJONSKANAL_CACHE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class DokdistkanalClient(
    private val dokDistKanalHttpClient: RestClient,
    @Value($$"${app.app-name}") private val applicationName: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Cacheable(DISTRIBUSJONSKANAL_CACHE)
    fun bestemDistribusjonskanal(personident: String, deltakerId: UUID? = null): Distribusjonskanal = dokDistKanalHttpClient
        .post()
        .uri("/rest/bestemDistribusjonskanal")
        .header(NAV_CALL_ID_HEADER_KEY, applicationName)
        .body(BestemDistribusjonskanalRequest(personident))
        .exchangeForRequiredValue { _, response ->
            if (response.statusCode.is2xxSuccessful) {
                log.debug("Hentet distribusjonskanal for personident")
                response
                    .bodyTo(BestemDistribusjonskanalResponse::class.java)
                    ?.distribusjonskanal
                    ?: error("Tom respons ved henting av distribusjonskanal")
            } else {
                val bodyText = response.bodyTo(String::class.java)

                if (deltakerId == null) {
                    error("Kunne ikke hente distribusjonskanal, status: ${response.statusCode} $bodyText")
                } else {
                    error("Kunne ikke hente distribusjonskanal for deltaker $deltakerId status: ${response.statusCode} $bodyText")
                }
            }
        }
}

data class BestemDistribusjonskanalRequest(
    val brukerId: String,
    val mottakerId: String = brukerId,
    val tema: String = "OPP",
    val erArkivert: Boolean = true, // hvis denne utelates eller settes til false så defaulter responsen til PRINT
)

data class BestemDistribusjonskanalResponse(
    val distribusjonskanal: Distribusjonskanal,
)
