package no.nav.amt.distribusjon.amtdeltaker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.testEnvironment
import no.nav.amt.distribusjon.utils.createMockHttpClient
import no.nav.amt.distribusjon.utils.data.DeltakerData.lagDeltaker
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AmtDeltakerClientTest {
    val mockAzureAdTokenClient: AzureAdTokenClient = mockk(relaxed = true)

    lateinit var sut: AmtDeltakerClient

    @Before
    fun setup() {
        coEvery { mockAzureAdTokenClient.getMachineToMachineToken(any()) } returns "~token~"
    }

    @Test
    fun `skal returnere deltakerliste nar getDeltaker kalles med gyldig respons`() {
        val deltakerId = UUID.randomUUID()
        val expectedDeltaker = lagDeltaker()

        sut = createAmtDeltakerClient(
            expectedUrl = "http://localhost/deltaker/$deltakerId",
            responseBody = expectedDeltaker,
        )

        val actualDeltaker = runBlocking {
            sut.getDeltaker(deltakerId)
        }

        actualDeltaker.deltakerliste shouldBe expectedDeltaker.deltakerliste
    }

    @Test
    fun `skal kaste feil nar getDeltaker returnerer feilkode`() {
        val deltakerId = UUID.randomUUID()

        sut = createAmtDeltakerClient(
            expectedUrl = "http://localhost/deltaker/$deltakerId",
            responseBody = null,
            HttpStatusCode.BadRequest,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.getDeltaker(deltakerId)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente deltaker fra amt-deltaker."
    }

    fun <T> createAmtDeltakerClient(
        expectedUrl: String,
        responseBody: T?,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ) = AmtDeltakerClient(
        httpClient = createMockHttpClient(expectedUrl, responseBody, statusCode),
        azureAdTokenClient = mockAzureAdTokenClient,
        environment = testEnvironment,
    )
}
