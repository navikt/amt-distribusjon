package no.nav.amt.distribusjon.journalforing.dokarkiv

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.deltaker.bff.utils.withLogCapture
import no.nav.amt.distribusjon.HttpClientTestBase
import no.nav.amt.distribusjon.utils.data.Hendelsesdata.tiltak
import no.nav.amt.distribusjon.veilarboppfolging.Sak
import no.nav.amt.distribusjon.veilarboppfolging.VeilarboppfolgingClientTest.Companion.oppfolgingsperiodeId
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

@RestClientTest(DokarkivClient::class)
@TestPropertySource(
    properties = [
        "app.dok-arkiv-url=http://localhost:8080",
    ],
)
class DokarkivClientTest(
    @Value($$"${app.dok-arkiv-url}") private val dokArkivUrl: String,
    private val sut: DokarkivClient,
) : HttpClientTestBase() {
    @Test
    fun `skal returnere journalpostId nar opprettJournalpost kalles med gyldig respons`() {
        mockServer
            .expect(requestTo(expectedUrl))
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

        val actualResponse = sut.opprettJournalpost(
            hendelseId = expectedHendelseId,
            fnr = "~fnr~",
            sak = Sak(oppfolgingsperiodeId, sakId = 42, fagsaksystem = "~fagsaksystem~"),
            pdf = "Hello World".toByteArray(),
            journalforendeEnhet = "~journalforendeEnhet~",
            tiltakstype = tiltak(),
            journalpostNavn = "~journalpostNavn~",
        )

        actualResponse shouldBe expectedResponse.journalpostId
    }

    @Test
    fun `skal skrive warning til loggene nar journalpost for hendelseId allerede er opprettet`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(
                withStatus(HttpStatusCode.valueOf(HttpStatus.CONFLICT.value()))
                    .body(objectMapper.writeValueAsString(expectedResponse))
                    .contentType(MediaType.APPLICATION_JSON),
            )

        withLogCapture("no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient") { logEvents ->
            val actualResponse = sut.opprettJournalpost(
                hendelseId = expectedHendelseId,
                fnr = "~fnr~",
                sak = Sak(oppfolgingsperiodeId, sakId = 42, fagsaksystem = "~fagsaksystem~"),
                pdf = "Hello World".toByteArray(),
                journalforendeEnhet = "~journalforendeEnhet~",
                tiltakstype = tiltak(),
                journalpostNavn = "~journalpostNavn~",
            )

            actualResponse shouldBe expectedResponse.journalpostId

            logEvents.shouldNotBeEmpty()
            logEvents.map { it.formattedMessage } shouldContain "Journalpost for hendelseId $expectedHendelseId er allerede opprettet"
        }
    }

    @Test
    fun `skal kaste feil nar opprettJournalpost returnerer feilkode`() {
        mockServer
            .expect(requestTo(expectedUrl))
            .andRespond(
                withForbiddenRequest(),
            )

        val thrown = shouldThrow<IllegalStateException> {
            sut.opprettJournalpost(
                hendelseId = expectedHendelseId,
                fnr = "~fnr~",
                sak = Sak(oppfolgingsperiodeId, sakId = 42, fagsaksystem = "~fagsaksystem~"),
                pdf = "Hello World".toByteArray(),
                journalforendeEnhet = "~journalforendeEnhet~",
                tiltakstype = tiltak(),
                journalpostNavn = "~journalpostNavn~",
            )
        }

        thrown.message shouldStartWith "Kunne ikke opprette journalpost for hendelseId $expectedHendelseId"
    }

    val expectedUrl =
        UriComponentsBuilder
            .fromUriString(dokArkivUrl)
            .pathSegment("rest")
            .pathSegment("journalpostapi")
            .pathSegment("v1")
            .pathSegment("journalpost")
            .queryParam("forsoekFerdigstill", "true")
            .build()
            .toUri()

    companion object {
        private val expectedHendelseId: UUID = UUID.randomUUID()
        private val expectedResponse = OpprettJournalpostResponse("~journalpostId~")
    }
}
