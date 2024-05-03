package no.nav.amt.distribusjon.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.toByteArray
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.applicationConfig
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.distribusjonskanal.BestemDistribusjonskanalRequest
import no.nav.amt.distribusjon.distribusjonskanal.BestemDistribusjonskanalResponse
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.distribusjonskanal.DokdistkanalClient
import no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient
import no.nav.amt.distribusjon.journalforing.dokarkiv.OpprettJournalpostResponse
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.journalforing.person.model.NavBruker
import no.nav.amt.distribusjon.journalforing.sak.Sak
import no.nav.amt.distribusjon.journalforing.sak.SakClient
import no.nav.amt.distribusjon.testEnvironment
import no.nav.amt.distribusjon.utils.data.Journalforingdata
import no.nav.amt.distribusjon.utils.data.Persondata

fun mockHttpClient(defaultResponse: Any? = null): HttpClient {
    val mockEngine = MockEngine {
        val body = when (it.body) {
            is TextContent -> {
                (it.body as TextContent).text
            }

            is ByteArrayContent -> {
                (it.body as ByteArrayContent).toByteArray().decodeToString()
            }

            else -> {
                null
            }
        }

        val request = MockResponseHandler.Request(it.url.toString(), it.method, body)

        val response = MockResponseHandler.responses.getOrPut(request) {
            MockResponseHandler.Response(
                if (defaultResponse is String) defaultResponse else objectMapper.writeValueAsString(defaultResponse),
                HttpStatusCode.OK,
            )
        }

        respond(
            content = ByteReadChannel(response.content),
            status = response.status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    return HttpClient(mockEngine) {
        install(ContentNegotiation) {
            jackson { applicationConfig() }
        }
    }
}

fun mockAzureAdClient(environment: Environment) = AzureAdTokenClient(
    httpClient = mockHttpClient(
        """
        {
            "token_type":"Bearer",
            "access_token":"XYZ",
            "expires_in": 3599
        }
        """.trimIndent(),
    ),
    environment,
)

fun mockPdfgenClient(environment: Environment) = PdfgenClient(
    mockHttpClient("%PDF-".toByteArray()),
    environment,
)

fun mockAmtPersonClient(
    azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
    navBruker: NavBruker = Persondata.lagNavBruker(),
) = AmtPersonClient(
    mockHttpClient(navBruker),
    azureAdTokenClient,
    environment,
)

fun mockSakClient(
    azureAdTokenClient: AzureAdTokenClient,
    environment: Environment,
    sak: Sak = Journalforingdata.lagSak(),
) = SakClient(
    mockHttpClient(sak),
    azureAdTokenClient,
    environment,
)

fun mockDokarkivClient(azureAdTokenClient: AzureAdTokenClient, environment: Environment) = DokarkivClient(
    mockHttpClient(OpprettJournalpostResponse("12345")),
    azureAdTokenClient,
    environment,
)

fun mockDokdistkanalClient(azureAdTokenClient: AzureAdTokenClient, environment: Environment) = DokdistkanalClient(
    mockHttpClient(BestemDistribusjonskanalResponse(Distribusjonskanal.DITT_NAV)),
    azureAdTokenClient,
    environment,
)

object MockResponseHandler {
    data class Request(
        val url: String,
        val method: HttpMethod,
        val body: String?,
    )

    data class Response(
        val content: String,
        val status: HttpStatusCode,
    )

    val responses = mutableMapOf<Request, Response>()

    fun addResponse(
        request: Request,
        responseBody: Any = "",
        responseCode: HttpStatusCode = HttpStatusCode.OK,
    ) {
        responses[request] = Response(
            if (responseBody is String) responseBody else objectMapper.writeValueAsString(responseBody),
            responseCode,
        )
    }

    fun addDistribusjonskanalResponse(personident: String, distribusjonskanal: Distribusjonskanal) {
        val url = "${testEnvironment.dokdistkanalUrl}/rest/bestemDistribusjonskanal"
        val request = Request(url, HttpMethod.Post, objectMapper.writeValueAsString(BestemDistribusjonskanalRequest(personident)))
        addResponse(request, BestemDistribusjonskanalResponse(distribusjonskanal))
    }
}
