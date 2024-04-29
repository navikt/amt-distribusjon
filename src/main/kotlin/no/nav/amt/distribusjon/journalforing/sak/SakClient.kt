package no.nav.amt.distribusjon.journalforing.sak

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import java.util.UUID

class SakClient(
    private val httpClient: HttpClient,
    private val azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
) {
    private val scope = environment.sakScope
    private val url = environment.sakUrl

    suspend fun opprettEllerHentSak(oppfolgingsperiodeId: UUID): Sak {
        val token = azureAdTokenClient.getMachineToMachineToken(scope)
        val response = httpClient.post("$url/veilarboppfolging/api/v3/sak/$oppfolgingsperiodeId") {
            header(HttpHeaders.Authorization, token)
            contentType(ContentType.Application.Json)
        }
        if (!response.status.isSuccess()) {
            error("Kunne ikke hente sak fra veilarboppfolging for oppfolgingsperiode $oppfolgingsperiodeId")
        }
        return response.body()
    }
}

data class Sak(
    val oppfolgingsperiodeId: UUID,
    val sakId: Long,
    val fagsaksystem: String,
    val tema: String,
)
