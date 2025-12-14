package no.nav.amt.distribusjon.veilarboppfolging

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.distribusjon.AppConstants.APPLICATION_NAME
import no.nav.amt.distribusjon.AppConstants.NAV_CONSUMER_ID_HEADER_KEY
import no.nav.amt.distribusjon.HttpClientTestBase
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
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.util.UUID

@RestClientTest(VeilarboppfolgingClient::class)
@TestPropertySource(
    properties = [
        "app.app-name=$APPLICATION_NAME",
        "app.veilarbo-url=http://localhost",
    ],
)
class VeilarboppfolgingClientTest(
    @Value($$"${app.app-name}") private val applicationName: String,
    @Value($$"${app.veilarbo-url}") private val veilarboUrl: String,
    private val sut: VeilarboppfolgingClient,
) : HttpClientTestBase() {
    @Test
    fun `skal returnere sak nar opprettEllerHentSak kalles med gyldig respons`() {
        val expectedSak = Sak(oppfolgingsperiodeId, sakId = 42, fagsaksystem = "~fagsaksystem~")

        mockServer
            .expect(requestTo("$veilarboUrl/veilarboppfolging/api/v3/sak/$oppfolgingsperiodeId"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(NAV_CONSUMER_ID_HEADER_KEY, applicationName))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(expectedSak),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val actualResponse = sut.opprettEllerHentSak(oppfolgingsperiodeId)

        actualResponse shouldBe expectedSak
    }

    @Test
    fun `skal kaste feil nar opprettEllerHentSak returnerer feilkode`() {
        mockServer
            .expect(requestTo("$veilarboUrl/veilarboppfolging/api/v3/sak/$oppfolgingsperiodeId"))
            .andRespond(withServerError())

        val thrown = shouldThrow<IllegalStateException> {
            sut.opprettEllerHentSak(oppfolgingsperiodeId)
        }

        thrown.message shouldStartWith "Kunne ikke hente sak fra veilarboppfolging for oppfolgingsperiode $oppfolgingsperiodeId"
    }

    @Test
    fun `skal returnere true nar bruker er under manuell oppfolging`() {
        mockServer
            .expect(requestTo("$veilarboUrl/veilarboppfolging/api/v3/hent-manuell"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(NAV_CONSUMER_ID_HEADER_KEY, applicationName))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(ManuellV2Response(true)),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = sut.erUnderManuellOppfolging("~personident~")

        result shouldBe true
    }

    @Test
    fun `skal returnere false nar bruker ikke er under manuell oppfolging`() {
        mockServer
            .expect(requestTo("$veilarboUrl/veilarboppfolging/api/v3/hent-manuell"))
            .andRespond(
                withSuccess(
                    objectMapper.writeValueAsString(ManuellV2Response(false)),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = sut.erUnderManuellOppfolging("~personident~")

        result shouldBe false
    }

    @Test
    fun `skal kaste feil nar erUnderManuellOppfolging returnerer feilkode`() {
        mockServer
            .expect(requestTo("$veilarboUrl/veilarboppfolging/api/v3/hent-manuell"))
            .andRespond(withServerError())

        val thrown = shouldThrow<IllegalStateException> {
            sut.erUnderManuellOppfolging("~personident~")
        }

        thrown.message shouldStartWith "Kunne ikke hente manuell oppfølging fra veilarboppfolging"
    }

    companion object {
        val oppfolgingsperiodeId: UUID = UUID.randomUUID()
    }
}
