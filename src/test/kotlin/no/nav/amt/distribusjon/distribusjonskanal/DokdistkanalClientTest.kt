package no.nav.amt.distribusjon.distribusjonskanal

import com.github.benmanes.caffeine.cache.Cache
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.ClientTestBase
import no.nav.amt.distribusjon.testEnvironment
import no.nav.amt.distribusjon.utils.createMockHttpClient
import org.junit.Before
import org.junit.Test
import java.util.UUID

class DokdistkanalClientTest : ClientTestBase() {
    private val mockDistribusjonskanalCache: Cache<String, Distribusjonskanal> = mockk(relaxed = true)

    @Before
    fun localSetup() {
        coEvery { mockDistribusjonskanalCache.getIfPresent(any()) } returns null
    }

    @Test
    fun `skal returnere DITT_NAV nar bestemDistribusjonskanal kalles med `() {
        val sut = createDokdistkanalClient(
            responseBody = expectedResponse,
        )

        val actualResponse = runBlocking {
            sut.bestemDistribusjonskanal(PERSON_IDENT, deltakerId)
        }

        actualResponse shouldBe expectedResponse.distribusjonskanal
    }

    @Test
    fun `skal bruke cache ved andre kall til bestemDistribusjonskanal`() {
        coEvery {
            mockDistribusjonskanalCache.getIfPresent(any())
        } returns null andThen expectedResponse.distribusjonskanal

        val sut = createDokdistkanalClient(
            responseBody = expectedResponse,
        )

        runBlocking {
            sut.bestemDistribusjonskanal(PERSON_IDENT, null)
            sut.bestemDistribusjonskanal(PERSON_IDENT, null)
        }

        coVerify(exactly = 1) { mockAzureAdTokenClient.getMachineToMachineToken(any()) }
    }

    @Test
    fun `skal kaste feil nar bestemDistribusjonskanal returnerer feilkode, deltakerId != null`() {
        val sut = createDokdistkanalClient(statusCode = HttpStatusCode.BadGateway)

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.bestemDistribusjonskanal(PERSON_IDENT, deltakerId)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente distribusjonskanal for deltaker"
    }

    @Test
    fun `skal kaste feil nar bestemDistribusjonskanal returnerer feilkode, deltakerId = null`() {
        val sut = createDokdistkanalClient(statusCode = HttpStatusCode.BadGateway)

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.bestemDistribusjonskanal(PERSON_IDENT, null)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente distribusjonskanal, status"
    }

    private fun createDokdistkanalClient(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        responseBody: BestemDistribusjonskanalResponse? = null,
    ) = DokdistkanalClient(
        httpClient = createMockHttpClient(
            expectedUrl = "http://dokdistkanal/rest/bestemDistribusjonskanal",
            statusCode = statusCode,
            responseBody = responseBody,
        ),
        azureAdTokenClient = mockAzureAdTokenClient,
        environment = testEnvironment,
        distribusjonskanalCache = mockDistribusjonskanalCache,
    )

    companion object {
        private const val PERSON_IDENT = "~personident~"
        private val deltakerId: UUID = UUID.randomUUID()
        private val expectedResponse = BestemDistribusjonskanalResponse(Distribusjonskanal.DITT_NAV)
    }
}
