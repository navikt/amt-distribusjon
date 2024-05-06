package no.nav.amt.distribusjon.journalforing.dokdistfordeling

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import org.slf4j.LoggerFactory
import java.util.UUID

class DokdistfordelingClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
) {
    private val scope = environment.safScope
    private val url = environment.dokdistfordelingUrl
    private val navCallId = Environment.appName
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun distribuerJournalpost(journalpostId: String, tvingSentralPrint: Boolean = false): UUID {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$url/rest/v1/distribuerjournalpost") {
            header(HttpHeaders.Authorization, token)
            header("Nav-Callid", navCallId)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(DistribuerJournalpostRequest(journalpostId, tvingSentralPrint)))
        }

        if (!response.status.isSuccess()) {
            error("Distrubering av journalpost $journalpostId feilet: ${response.status}")
        }

        val bestillingsId = response.body<DistribuerJournalpostResponse>().bestillingsId

        log.info("Distribuerte journalpost $journalpostId, bestillingsId=$bestillingsId")

        return bestillingsId
    }
}

data class DistribuerJournalpostRequest(
    val journalpostId: String,
    val tvingSentralPrint: Boolean = false,
    val dokumentProdApp: String = Environment.appName,
    val distribusjonstype: Distribusjonstype = Distribusjonstype.VEDTAK,
    val distribusjonstidspunkt: Distribusjonstidspunkt = Distribusjonstidspunkt.UMIDDELBART,
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
