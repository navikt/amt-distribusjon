package no.nav.amt.distribusjon.distribusjonskanal

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
import no.nav.amt.distribusjon.hendelse.model.Hendelse

class DokdistkanalClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
) {
    private val scope = environment.dokdistkanalScope
    private val url = environment.dokdistkanalUrl
    private val navCallId = "amt-distribusjon"

    suspend fun bestemDistribusjonskanal(hendelse: Hendelse): Distribusjonskanal {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$url/rest/bestemDistribusjonskanal") {
            header(HttpHeaders.Authorization, token)
            header("Nav-Callid", navCallId)
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(BestemDistribusjonskanalRequest(hendelse.deltaker.personident)))
        }
        if (!response.status.isSuccess()) {
            error("Kunne ikke hente distribusjonskanal journalpost for hendelseId ${hendelse.id}")
        }
        return response.body<BestemDistribusjonskanalResponse>().distribusjonskanal
    }
}

data class BestemDistribusjonskanalRequest(
    val brukerId: String,
    val mottakerId: String = brukerId,
    val tema: String = "OPP",
)

data class BestemDistribusjonskanalResponse(
    val distribusjonskanal: Distribusjonskanal,
)
