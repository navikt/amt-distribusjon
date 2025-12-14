package no.nav.amt.distribusjon.amtdeltaker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.distribusjon.HttpClientTestBase
import no.nav.amt.distribusjon.utils.data.DeltakerData.lagDeltaker
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.util.UriComponentsBuilder

@RestClientTest(AmtDeltakerClient::class)
@TestPropertySource(
    properties = [
        "app.amt-deltaker-url=http://localhost:8080",
    ],
)
class AmtDeltakerClientTest(
    @Value($$"${app.amt-deltaker-url}") private val amtDeltakerUrl: String,
    private val sut: AmtDeltakerClient,
) : HttpClientTestBase() {
    @Test
    fun `skal returnere deltakerliste nar getDeltaker kalles med gyldig respons`() {
        val expectedResponseBody = objectMapper.writeValueAsString(expectedDeltaker)

        mockServer
            .expect(requestTo(expectedUrl))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(expectedResponseBody, MediaType.APPLICATION_JSON),
            )

        val actualDeltaker = sut.getDeltaker(expectedDeltaker.id)

        actualDeltaker.deltakerliste shouldBe expectedDeltaker.deltakerliste
    }

    @Test
    fun `skal kaste feil nar getDeltaker returnerer feilkode`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(
                withForbiddenRequest(),
            )

        val thrown =
            shouldThrow<IllegalStateException> {
                sut.getDeltaker(expectedDeltaker.id)
            }

        thrown.message shouldStartWith "Kunne ikke hente deltaker fra amt-deltaker. Metode: GET, status: 403 FORBIDDEN, error="
    }

    private val expectedUrl =
        UriComponentsBuilder
            .fromUriString(amtDeltakerUrl)
            .pathSegment("deltaker")
            .pathSegment(expectedDeltaker.id.toString())
            .build()
            .toUri()

    companion object {
        private val expectedDeltaker = lagDeltaker()
    }
}
