package no.nav.amt.distribusjon.distribusjonskanal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.distribusjon.AppConstants.APPLICATION_NAME
import no.nav.amt.distribusjon.AppConstants.NAV_CALL_ID_HEADER_KEY
import no.nav.amt.distribusjon.HttpClientTestBase
import no.nav.amt.distribusjon.config.CacheConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@RestClientTest(DokdistkanalClient::class)
@Import(CacheConfig::class)
@TestPropertySource(
    properties = [
        "app.app-name=$APPLICATION_NAME",
        "app.dok-dist-kanal-url=http://localhost:8080",
    ],
)
class DokdistkanalClientTest(
    @Value($$"${app.dok-dist-kanal-url}") private val dokDistKanalUrl: String,
    @Value($$"${app.app-name}") private val applicationName: String,
    private val sut: DokdistkanalClient,
) : HttpClientTestBase() {
    @BeforeEach
    fun setup() = mockServer.reset()

    @Test
    fun `skal returnere DITT_NAV nar bestemDistribusjonskanal kalles med `() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(NAV_CALL_ID_HEADER_KEY, applicationName))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(expectedResponse),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val actualResponse = sut.bestemDistribusjonskanal(PERSON_IDENT, deltakerId)

        actualResponse shouldBe expectedResponse.distribusjonskanal
    }

    @Test
    fun `skal bruke cache ved andre kall til bestemDistribusjonskanal`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(expectedResponse),
                    MediaType.APPLICATION_JSON,
                ),
            )

        withLogCapture("no.nav.amt.distribusjon.distribusjonskanal.DokdistkanalClient") { logEvents ->
            sut.bestemDistribusjonskanal(PERSON_IDENT, null)
            sut.bestemDistribusjonskanal(PERSON_IDENT, null)

            logEvents.size shouldBe 1
            logEvents.map { it.formattedMessage } shouldContain "Hentet distribusjonskanal for personident"
        }
    }

    @Test
    fun `skal kaste feil nar bestemDistribusjonskanal returnerer feilkode, deltakerId != null`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(withServerError())

        val thrown = shouldThrow<IllegalStateException> {
            sut.bestemDistribusjonskanal("$PERSON_IDENT-1", deltakerId)
        }

        thrown.message shouldStartWith "Kunne ikke hente distribusjonskanal for deltaker"
    }

    @Test
    fun `skal kaste feil nar bestemDistribusjonskanal returnerer feilkode, deltakerId = null`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(withServerError())

        val thrown = shouldThrow<IllegalStateException> {
            sut.bestemDistribusjonskanal("$PERSON_IDENT-2", null)
        }

        thrown.message shouldStartWith "Kunne ikke hente distribusjonskanal, status"
    }

    private val expectedUrl =
        UriComponentsBuilder
            .fromUriString(dokDistKanalUrl)
            .pathSegment("rest")
            .pathSegment("bestemDistribusjonskanal")
            .build()
            .toUri()

    companion object {
        private const val PERSON_IDENT = "~personident~"
        private val deltakerId: UUID = UUID.randomUUID()
        private val expectedResponse = BestemDistribusjonskanalResponse(Distribusjonskanal.DITT_NAV)
    }
}
