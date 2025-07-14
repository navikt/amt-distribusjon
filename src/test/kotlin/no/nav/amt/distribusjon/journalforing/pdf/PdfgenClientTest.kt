package no.nav.amt.distribusjon.journalforing.pdf

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.testEnvironment
import no.nav.amt.distribusjon.utils.ClientTestBase
import no.nav.amt.distribusjon.utils.createMockHttpClient
import no.nav.amt.distribusjon.utils.data.HendelseTypeData.utkast
import no.nav.amt.distribusjon.utils.data.Hendelsesdata.ansvarligNavVeileder
import no.nav.amt.distribusjon.utils.data.Hendelsesdata.deltaker
import no.nav.amt.distribusjon.utils.data.Persondata
import no.nav.amt.lib.models.hendelse.HendelseAnsvarlig
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PdfgenClientTest : ClientTestBase() {
    @Test
    fun `skal returnere ByteArray nar genererHovedvedtak kalles med gyldig respons`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_HOVEDVEDTAK_URL,
            responseBody = expectedResponse,
        )

        val actualResponse = runBlocking {
            sut.genererHovedvedtak(hovedVedtakPdfDto)
        }

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererHovedvedtak returnerer feilkode`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_HOVEDVEDTAK_URL,
            statusCode = HttpStatusCode.BadGateway,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.genererHovedvedtak(hovedVedtakPdfDto)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente opprette hovedvedtak-pdf i amt-pdfgen."
    }

    @Test
    fun `skal returnere ByteArray nar genererHovedvedtakFellesOppstart kalles med gyldig respons`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_HOVEDVEDTAK_FELLES_OPPSTART_URL,
            responseBody = expectedResponse,
        )

        val actualResponse = runBlocking {
            sut.genererHovedvedtakFellesOppstart(hovedopptakFellesOppstart)
        }

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererHovedvedtakFellesOppstart returnerer feilkode`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_HOVEDVEDTAK_FELLES_OPPSTART_URL,
            statusCode = HttpStatusCode.BadGateway,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.genererHovedvedtakFellesOppstart(hovedopptakFellesOppstart)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente opprette hovedvedtak-pdf i amt-pdfgen."
    }

    @Test
    fun `skal returnere ByteArray nar genererInnsokingsbrevPDF kalles med gyldig respons`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_INNSOKINGSBREV_PDF_URL,
            responseBody = expectedResponse,
        )

        val actualResponse = runBlocking {
            sut.genererInnsokingsbrevPDF(innsokingsbrevPdfDto)
        }

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererInnsokingsbrevPDF returnerer feilkode`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_INNSOKINGSBREV_PDF_URL,
            statusCode = HttpStatusCode.BadGateway,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.genererInnsokingsbrevPDF(innsokingsbrevPdfDto)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente opprette kurs-innsoking-pdf i amt-pdfgen."
    }

    @Test
    fun `skal returnere ByteArray nar genererVentelistebrevPDF kalles med gyldig respons`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_VENTELISTEBREV_PDF_URL,
            responseBody = expectedResponse,
        )

        val actualResponse = runBlocking {
            sut.genererVentelistebrevPDF(ventelistebrevPdfDto)
        }

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar genererVentelistebrevPDF returnerer feilkode`() {
        val sut = createPdfgenClient(
            expectedUrl = GENERER_VENTELISTEBREV_PDF_URL,
            statusCode = HttpStatusCode.BadGateway,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.genererVentelistebrevPDF(ventelistebrevPdfDto)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente opprette venteliste-pdf i amt-pdfgen."
    }

    @Test
    fun `skal returnere ByteArray nar endringsvedtak kalles med gyldig respons`() {
        val sut = createPdfgenClient(
            expectedUrl = ENDRINGSVEDTAK_URL,
            responseBody = expectedResponse,
        )

        val actualResponse = runBlocking {
            sut.endringsvedtak(endringsvedtakPdfDto)
        }

        actualResponse shouldBe expectedResponse
    }

    @Test
    fun `skal kaste feil nar endringsvedtak returnerer feilkode`() {
        val sut = createPdfgenClient(
            expectedUrl = ENDRINGSVEDTAK_URL,
            statusCode = HttpStatusCode.BadGateway,
        )

        val thrown = runBlocking {
            shouldThrow<IllegalStateException> {
                sut.endringsvedtak(endringsvedtakPdfDto)
            }
        }

        thrown.message shouldStartWith "Kunne ikke hente opprette endringsvedtak-pdf i amt-pdfgen."
    }

    private fun createPdfgenClient(
        expectedUrl: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        responseBody: ByteArray? = null,
    ) = PdfgenClient(
        httpClient = createMockHttpClient(
            expectedUrl = expectedUrl,
            responseBody = responseBody,
            statusCode = statusCode,
            requiresAuthHeader = false,
        ),
        environment = testEnvironment,
    )

    companion object {
        private val expectedResponse = "Hello World!".toByteArray()

        private const val GENERER_HOVEDVEDTAK_URL = "http://localhost/api/v1/genpdf/amt/hovedvedtak"
        private const val GENERER_HOVEDVEDTAK_FELLES_OPPSTART_URL = "http://localhost/api/v1/genpdf/amt/hovedvedtak-felles-oppstart"
        private const val GENERER_INNSOKINGSBREV_PDF_URL = "http://localhost/api/v1/genpdf/amt/innsokingsbrev"
        private const val GENERER_VENTELISTEBREV_PDF_URL = "http://localhost/api/v1/genpdf/amt/ventelistebrev"
        private const val ENDRINGSVEDTAK_URL = "http://localhost/api/v1/genpdf/amt/endringsvedtak"

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
