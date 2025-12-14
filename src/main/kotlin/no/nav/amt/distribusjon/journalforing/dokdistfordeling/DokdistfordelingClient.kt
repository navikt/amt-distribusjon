package no.nav.amt.distribusjon.journalforing.dokdistfordeling

import no.nav.amt.distribusjon.AppConstants.NAV_CALL_ID_HEADER_KEY
import no.nav.amt.distribusjon.Environment
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class DokdistfordelingClient(
    private val dokDistFordelingHttpClient: RestClient,
    @Value($$"${app.app-name}") private val applicationName: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /*
        https://confluence.adeo.no/pages/viewpage.action?pageId=320038938
        distribusjon av dokumenter knyttet til en utgående, ferdigstilt journalpost.
        Denne brukes kun dersom brukeren ikke er digital så resultatet blir i praksis at det sendes brev
     */
    fun distribuerJournalpost(
        journalpostId: String,
        distribusjonstype: DistribuerJournalpostRequest.Distribusjonstype = DistribuerJournalpostRequest.Distribusjonstype.VEDTAK,
        tvingSentralPrint: Boolean = false,
    ): UUID? = dokDistFordelingHttpClient
        .post()
        .uri("/rest/v1/distribuerjournalpost")
        .header(NAV_CALL_ID_HEADER_KEY, applicationName)
        .body(
            DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                tvingSentralPrint = tvingSentralPrint,
                distribusjonstype = distribusjonstype,
            ),
        ).exchange { _, response ->
            when (response.statusCode.value()) {
                in 200..299 -> {
                    response
                        .bodyTo(DistribuerJournalpostResponse::class.java)
                        ?.bestillingsId
                        ?.also { log.info("Distribuerte journalpost $journalpostId, bestillingsId=$it") }
                        ?: error("Tom respons ved distribuering av journalpost $journalpostId")
                }

                HttpStatus.CONFLICT.value() -> {
                    log.warn("Journalpost $journalpostId er allerede distribuert")
                    response.bodyTo(DistribuerJournalpostResponse::class.java)?.bestillingsId
                }

                HttpStatus.GONE.value() -> {
                    log.warn(
                        "Journalpost $journalpostId tilhører bruker som er død og som mangler adresse i PDL. Kan ikke sende brev.",
                    )
                    null
                }

                else -> {
                    val bodyText = response.bodyTo(String::class.java)

                    throw IllegalStateException(
                        "Distribuering av journalpost $journalpostId feilet: " +
                            "Status=${response.statusCode} error=$bodyText",
                    )
                }
            }
        }
}

data class DistribuerJournalpostRequest(
    val journalpostId: String,
    val tvingSentralPrint: Boolean = false,
    val bestillendeFagsystem: String = Environment.appName,
    val dokumentProdApp: String = Environment.appName,
    val distribusjonstype: Distribusjonstype = Distribusjonstype.VEDTAK,
    val distribusjonstidspunkt: Distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
) {
    enum class Distribusjonstidspunkt {
        UMIDDELBART,
        KJERNETID,
    }

    enum class Distribusjonstype {
        VEDTAK,
        VIKTIG,
        ANNET,
    }
}

data class DistribuerJournalpostResponse(
    val bestillingsId: UUID,
)
