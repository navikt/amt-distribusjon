package no.nav.amt.distribusjon.journalforing.pdf

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper

class PdfgenClient(private val httpClient: HttpClient, private val environment: Environment) {
    private val url = environment.amtPdfgenUrl + "/api/v1/genpdf/amt"

    suspend fun hovedvedtak(hovedvedtakPdfDto: HovedvedtakPdfDto): ByteArray {
        val response = httpClient.post("$url/hovedvedtak") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(hovedvedtakPdfDto))
        }
        if (!response.status.isSuccess()) {
            error("Kunne ikke hente opprette hovedvedtak-pdf i amt-pdfgen. Status=${response.status.value} error=${response.bodyAsText()}")
        }
        return response.body()
    }

    suspend fun endringsvedtak(endringsvedtakPdfDto: EndringsvedtakPdfDto): ByteArray {
        val response = httpClient.post("$url/endringsvedtak") {
            contentType(ContentType.Application.Json)
            setBody(objectMapper.writeValueAsString(endringsvedtakPdfDto))
        }
        if (!response.status.isSuccess()) {
            error(
                "Kunne ikke hente opprette endringsvedtak-pdf i amt-pdfgen. Status=${response.status.value} error=${response.bodyAsText()}",
            )
        }
        return response.body()
    }
}
