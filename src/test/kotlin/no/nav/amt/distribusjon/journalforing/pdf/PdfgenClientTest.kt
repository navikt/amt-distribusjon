package no.nav.amt.distribusjon.journalforing.pdf

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.amt.distribusjon.HttpClientTestBase
import no.nav.amt.distribusjon.utils.data.HendelseTypeData.utkast
import no.nav.amt.distribusjon.utils.data.Hendelsesdata.ansvarligNavVeileder
import no.nav.amt.distribusjon.utils.data.Hendelsesdata.deltaker
import no.nav.amt.distribusjon.utils.data.Persondata
import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig
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
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import java.time.LocalDate
import java.util.UUID

@RestClientTest(PdfgenClient::class)
@TestPropertySource(
    properties = [
        "app.pdf-gen-url=http://localhost/api/v1/genpdf/amt",
    ],
)
class PdfgenClientTest(
    @Value($$"${app.pdf-gen-url}") private val pdfGenUrl: String,
    private val sut: PdfgenClient,
) : HttpClientTestBase() {
    @Test
    fun `skal returnere ByteArray nar genererHovedvedtak kalles med gyldig respons`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/hovedvedtak"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(expectedResponse, MediaType.APPLICATION_PDF),
            )

        val actualResponse = sut.genererHovedvedtak(hovedVedtakPdfDto)

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererHovedvedtak returnerer feilkode`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/hovedvedtak"))
            .andRespond(withForbiddenRequest())

        val thrown = shouldThrow<IllegalStateException> {
            sut.genererHovedvedtak(hovedVedtakPdfDto)
        }

        thrown.message shouldStartWith "Kunne ikke hente/opprette hovedvedtak-PDF i amt-pdfgen."
    }

    @Test
    fun `skal returnere ByteArray nar genererHovedvedtakFellesOppstart kalles med gyldig respons`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/hovedvedtak-felles-oppstart"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(expectedResponse, MediaType.APPLICATION_PDF),
            )

        val actualResponse = sut.genererHovedvedtakFellesOppstart(hovedopptakFellesOppstart)

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererHovedvedtakFellesOppstart returnerer feilkode`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/hovedvedtak-felles-oppstart"))
            .andRespond(withServerError())

        val thrown = shouldThrow<IllegalStateException> {
            sut.genererHovedvedtakFellesOppstart(hovedopptakFellesOppstart)
        }

        thrown.message shouldStartWith "Kunne ikke hente/opprette felles oppstart hovedvedtak-PDF i amt-pdfgen"
    }

    @Test
    fun `skal returnere ByteArray nar genererInnsokingsbrevPDF kalles med gyldig respons`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/innsokingsbrev"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(expectedResponse, MediaType.APPLICATION_PDF),
            )

        val actualResponse = sut.genererInnsokingsbrevPDF(innsokingsbrevPdfDto)

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererInnsokingsbrevPDF returnerer feilkode`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/innsokingsbrev"))
            .andRespond(withUnauthorizedRequest())

        val thrown = shouldThrow<IllegalStateException> {
            sut.genererInnsokingsbrevPDF(innsokingsbrevPdfDto)
        }

        thrown.message shouldStartWith "Kunne ikke hente/opprette kurs-innsoking-PDF i amt-pdfgen"
    }

    @Test
    fun `skal returnere ByteArray nar genererVentelistebrevPDF kalles med gyldig respons`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/ventelistebrev"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(expectedResponse, MediaType.APPLICATION_PDF),
            )

        val actualResponse = sut.genererVentelistebrevPDF(ventelistebrevPdfDto)

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererVentelistebrevPDF returnerer feilkode`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/ventelistebrev"))
            .andRespond(withUnauthorizedRequest())

        val thrown = shouldThrow<IllegalStateException> {
            sut.genererVentelistebrevPDF(ventelistebrevPdfDto)
        }

        thrown.message shouldStartWith "Kunne ikke hente/opprette venteliste-PDF i amt-pdfgen"
    }

    @Test
    fun `skal returnere ByteArray nar endringsvedtak kalles med gyldig respons`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/endringsvedtak"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer $MOCKED_TOKEN"))
            .andRespond(
                withSuccess(expectedResponse, MediaType.APPLICATION_PDF),
            )

        val actualResponse = sut.endringsvedtak(endringsvedtakPdfDto)

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar endringsvedtak returnerer feilkode`() {
        mockServer
            .expect(requestTo("$pdfGenUrl/endringsvedtak"))
            .andRespond(withUnauthorizedRequest())

        val thrown = shouldThrow<IllegalStateException> {
            sut.endringsvedtak(endringsvedtakPdfDto)
        }

        thrown.message shouldStartWith "Kunne ikke hente/opprette endringsvedtak-PDF i amt-pdfgen"
    }

    companion object {
        private val expectedResponse = "Hello World!".toByteArray()

        private val hovedVedtakPdfDto = lagHovedvedtakPdfDto(
            deltaker = deltaker(),
            navBruker = Persondata.lagNavBruker(),
            utkast = utkast(),
            veileder = ansvarligNavVeileder(),
            vedtaksdato = LocalDate.now(),
            begrunnelseFraNav = null,
        )

        private val ansvarlig = HendelseAnsvarlig.NavTiltakskoordinator(
            id = UUID.randomUUID(),
            navn = "~navn~",
            navIdent = "~navIdent~",
            enhet = HendelseAnsvarlig.NavTiltakskoordinator.Enhet(
                id = UUID.randomUUID(),
                enhetsnummer = "~enhetsnummer~",
                navn = "~navn~",
            ),
        )

        private val hovedopptakFellesOppstart = lagHovedopptakFellesOppstart(
            deltaker = deltaker(),
            navBruker = Persondata.lagNavBruker(),
            ansvarlig = ansvarlig,
            opprettetDato = LocalDate.now(),
        )

        private val innsokingsbrevPdfDto = lagInnsokingsbrevPdfDto(
            deltaker = deltaker(),
            navBruker = Persondata.lagNavBruker(),
            veileder = ansvarligNavVeileder(),
            opprettetDato = LocalDate.now(),
        )

        private val ventelistebrevPdfDto = lagVentelistebrevPdfDto(
            deltaker = deltaker(),
            navBruker = Persondata.lagNavBruker(),
            endretAv = ansvarlig,
            hendelseOpprettetDato = LocalDate.now(),
        )

        private val endringsvedtakPdfDto = lagEndringsvedtakPdfDto(
            deltaker = deltaker(),
            navBruker = Persondata.lagNavBruker(),
            ansvarlig = ansvarlig,
            hendelser = emptyList(),
            opprettetDato = LocalDate.now(),
        )
    }
}
