package no.nav.amt.distribusjon.journalforing.dokdistfordeling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.distribusjon.AppConstants.APPLICATION_NAME
import no.nav.amt.distribusjon.AppConstants.NAV_CALL_ID_HEADER_KEY
import no.nav.amt.distribusjon.HttpClientTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@RestClientTest(DokdistfordelingClient::class)
@TestPropertySource(
    properties = [
        "app.app-name=$APPLICATION_NAME",
        "app.dok-dist-fordeling-url=http://localhost:8080",
    ],
)
class DokdistfordelingClientTest(
    @Value($$"${app.dok-dist-fordeling-url}") private val dokDistFordelingUrl: String,
    @Value($$"${app.app-name}") private val applicationName: String,
    private val sut: DokdistfordelingClient,
) : HttpClientTestBase() {
    @Test
    fun `skal returnere bestillingsId nar distribuerJournalpost kalles med gyldig respons`() {
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

        val actualResponse = sut.distribuerJournalpost(
            journalpostId = JOURNAL_POST_ID,
            distribusjonstype = DistribuerJournalpostRequest.Distribusjonstype.VEDTAK,
            tvingSentralPrint = true,
        )

        actualResponse shouldBe expectedBestillingsId
    }

    @Test
    fun `skal returnere bestillingsId og logge warning nar distribuerJournalpost kalles med respons som returnerer 409 Conflict`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(
                withStatus(HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()))
                    .body(objectMapper.writeValueAsString(expectedResponse))
                    .contentType(MediaType.APPLICATION_JSON),
            )

        withLogCapture("no.nav.amt.distribusjon.journalforing.dokdistfordeling.DokdistfordelingClient") { logEvents ->
            val actualResponse = sut.distribuerJournalpost(
                journalpostId = JOURNAL_POST_ID,
                distribusjonstype = DistribuerJournalpostRequest.Distribusjonstype.VEDTAK,
                tvingSentralPrint = true,
            )

            actualResponse shouldBe expectedBestillingsId

            logEvents.map { it.formattedMessage } shouldContain "Journalpost $JOURNAL_POST_ID er allerede distribuert"
        }
    }

    @Test
    fun `skal returnere null og logge warning nar distribuerJournalpost kalles med respons som returnerer 410 Gone`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(withStatus(HttpStatusCode.valueOf(HttpStatus.GONE.value())))

        withLogCapture("no.nav.amt.distribusjon.journalforing.dokdistfordeling.DokdistfordelingClient") { logEvents ->
            val actualResponse = sut.distribuerJournalpost(
                journalpostId = JOURNAL_POST_ID,
                distribusjonstype = DistribuerJournalpostRequest.Distribusjonstype.VEDTAK,
                tvingSentralPrint = true,
            )

            actualResponse.shouldBeNull()

            logEvents.map { it.formattedMessage } shouldContain
                "Journalpost $JOURNAL_POST_ID tilhører bruker som er død og som mangler adresse i PDL. Kan ikke sende brev."
        }
    }

    @Test
    fun `skal kaste feil nar distribuerJournalpost kalles med response som returnerer feilkode forskjellig fra 409 og 410`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(withForbiddenRequest())

        val thrown = shouldThrow<IllegalStateException> {
            sut.distribuerJournalpost(
                journalpostId = JOURNAL_POST_ID,
                distribusjonstype = DistribuerJournalpostRequest.Distribusjonstype.VEDTAK,
                tvingSentralPrint = true,
            )
        }

        thrown.message shouldStartWith "Distribuering av journalpost $JOURNAL_POST_ID feilet"
    }

    val expectedUrl =
        UriComponentsBuilder
            .fromUriString(dokDistFordelingUrl)
            .pathSegment("rest")
            .pathSegment("v1")
            .pathSegment("distribuerjournalpost")
            .build()
            .toUri()

    companion object {
        private const val JOURNAL_POST_ID = "~journalpostId~"
        private val expectedBestillingsId: UUID = UUID.randomUUID()
        private val expectedResponse = DistribuerJournalpostResponse(expectedBestillingsId)
    }
}
