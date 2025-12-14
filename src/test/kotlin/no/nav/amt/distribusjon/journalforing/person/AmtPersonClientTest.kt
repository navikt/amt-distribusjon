package no.nav.amt.distribusjon.journalforing.person

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.distribusjon.HttpClientTestBase
import no.nav.amt.distribusjon.utils.data.Persondata.lagNavBruker
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

@RestClientTest(AmtPersonClient::class)
@TestPropertySource(
    properties = [
        "app.person-service-url=http://localhost",
    ],
)
class AmtPersonClientTest(
    @Value($$"${app.person-service-url}") private val personServiceUrl: String,
    private val sut: AmtPersonClient,
) : HttpClientTestBase() {
    @Test
    fun `skal returnere NavBruker nar hentNavBruker kalles med gyldig respons`() {
        val expectedResponse = lagNavBruker()

        mockServer
            .expect(requestTo("$personServiceUrl/api/nav-bruker"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(expectedResponse),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val actualResponse = sut.hentNavBruker("~personident~")

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar hentNavBruker returnerer feilkode`() {
        mockServer
            .expect(requestTo("$personServiceUrl/api/nav-bruker"))
            .andRespond(withForbiddenRequest())

        val thrown = shouldThrow<IllegalStateException> {
            sut.hentNavBruker("~personident~")
        }

        thrown.message shouldStartWith "Kunne ikke hente Nav-bruker fra amt-person-service"
    }
}
