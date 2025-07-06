package no.nav.amt.distribusjon.veilarboppfolging

import com.github.benmanes.caffeine.cache.Cache
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.testEnvironment
import no.nav.amt.lib.utils.objectMapper
import org.junit.Before
import org.junit.Test
import java.util.UUID

class VeilarboppfolgingClientTest {
    val mockAzureAdTokenClient: AzureAdTokenClient = mockk(relaxed = true)
    val mockManuellOppfolgingCache: Cache<String, Boolean> = mockk(relaxed = true)

    lateinit var sut: VeilarboppfolgingClient

    @Before
    fun setup() {
        coEvery { mockAzureAdTokenClient.getMachineToMachineToken(any()) } returns "~token~"
        coEvery { mockManuellOppfolgingCache.getIfPresent(any()) } returns null
    }

    @Test
    fun `skal returnere sak nar opprettEllerHentSak kalles med gyldig respons`() {
        val oppfolgingsperiodeId = UUID.randomUUID()
        val expectedSak = Sak(oppfolgingsperiodeId, sakId = 42, fagsaksystem = "~fagsaksystem~")

        sut = createVeilarboppfolgingClient(
            expectedUrl = "http://veilarboppfolging/veilarboppfolging/api/v3/sak/$oppfolgingsperiodeId",
            responseBody = expectedSak,
        )

        val actualSak = runBlocking {
            sut.opprettEllerHentSak(oppfolgingsperiodeId)
        }

        actualSak shouldBe expectedSak
    }

    @Test
    fun `skal kaste feil nar opprettEllerHentSak returnerer feilkode`() {
        val oppfolgingsperiodeId = UUID.randomUUID()

        sut = createVeilarboppfolgingClient(
            expectedUrl = "http://veilarboppfolging/veilarboppfolging/api/v3/sak/$oppfolgingsperiodeId",
            responseBody = "",
            statusCode = HttpStatusCode.BadRequest,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.opprettEllerHentSak(oppfolgingsperiodeId)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente sak fra veilarboppfolging for oppfolgingsperiode $oppfolgingsperiodeId"
    }

    @Test
    fun `skal returnere true nar bruker er under manuell oppfolging`() {
        sut = createVeilarboppfolgingClient(
            expectedUrl = "http://veilarboppfolging/veilarboppfolging/api/v3/hent-manuell",
            responseBody = ManuellV2Response(true),
        )

        val result = runBlocking {
            sut.erUnderManuellOppfolging("~personident~")
        }

        result shouldBe true
    }

    @Test
    fun `skal returnere false nar bruker ikke er under manuell oppfolging`() {
        sut = createVeilarboppfolgingClient(
            expectedUrl = "http://veilarboppfolging/veilarboppfolging/api/v3/hent-manuell",
            responseBody = ManuellV2Response(false),
        )

        val result = runBlocking {
            sut.erUnderManuellOppfolging("~personident~")
        }

        result shouldBe false
    }

    @Test
    fun `skal bruke cache ved andre kall til erUnderManuellOppfolging`() {
        coEvery {
            mockManuellOppfolgingCache.getIfPresent(any())
        } returns null andThen true

        sut = createVeilarboppfolgingClient(
            expectedUrl = "http://veilarboppfolging/veilarboppfolging/api/v3/hent-manuell",
            responseBody = ManuellV2Response(false),
        )

        runBlocking {
            sut.erUnderManuellOppfolging("~personident~")
            sut.erUnderManuellOppfolging("~personident~")
        }

        coVerify(exactly = 1) { mockAzureAdTokenClient.getMachineToMachineToken(any()) }
    }

    @Test
    fun `skal kaste feil nar erUnderManuellOppfolging returnerer feilkode`() {
        sut = createVeilarboppfolgingClient(
            expectedUrl = "http://veilarboppfolging/veilarboppfolging/api/v3/hent-manuell",
            responseBody = "",
            statusCode = HttpStatusCode.BadRequest,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.erUnderManuellOppfolging("~personident~")
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente manuell oppfølging fra veilarboppfolging"
    }

    fun <T : Any> createVeilarboppfolgingClient(
        expectedUrl: String,
        responseBody: T,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ): VeilarboppfolgingClient {
        val mockHttpClient = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                jackson()
            }
            engine {
                addHandler { request ->
                    request.url.toString() shouldBe expectedUrl
                    request.headers[HttpHeaders.Authorization] shouldBe "~token~"

                    respond(
                        content = objectMapper.writeValueAsString(responseBody),
                        status = statusCode,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }
        return VeilarboppfolgingClient(
            httpClient = mockHttpClient,
            azureAdTokenClient = mockAzureAdTokenClient,
            environment = testEnvironment,
            manuellOppfolgingCache = mockManuellOppfolgingCache,
        )
    }
}
