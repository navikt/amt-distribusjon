package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.exchangeWithLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class PdfgenClient(
    private val pdfGenHttpClient: RestClient,
) {
    fun genererHovedvedtak(hovedvedtakPdfDto: HovedvedtakPdfDto): ByteArray = pdfGenHttpClient
        .post()
        .uri("/hovedvedtak")
        .body(hovedvedtakPdfDto)
        .exchangeWithLogging("Kunne ikke hente/opprette hovedvedtak-PDF i amt-pdfgen")

    fun genererHovedvedtakFellesOppstart(hovedvedtakPdfDto: HovedvedtakFellesOppstartPdfDto): ByteArray = pdfGenHttpClient
        .post()
        .uri("/hovedvedtak-felles-oppstart")
        .body(hovedvedtakPdfDto)
        .exchangeWithLogging("Kunne ikke hente/opprette felles oppstart hovedvedtak-PDF i amt-pdfgen")

    fun genererInnsokingsbrevPDF(innsokingsbrevPdfDto: InnsokingsbrevPdfDto): ByteArray = pdfGenHttpClient
        .post()
        .uri("/innsokingsbrev")
        .body(innsokingsbrevPdfDto)
        .exchangeWithLogging("Kunne ikke hente/opprette kurs-innsoking-PDF i amt-pdfgen")

    fun genererVentelistebrevPDF(ventelistebrevPdfDto: VentelistebrevPdfDto): ByteArray = pdfGenHttpClient
        .post()
        .uri("/ventelistebrev")
        .body(ventelistebrevPdfDto)
        .exchangeWithLogging("Kunne ikke hente/opprette venteliste-PDF i amt-pdfgen")

    fun endringsvedtak(endringsvedtakPdfDto: EndringsvedtakPdfDto): ByteArray = pdfGenHttpClient
        .post()
        .uri("/endringsvedtak")
        .body(endringsvedtakPdfDto)
        .exchangeWithLogging("Kunne ikke hente/opprette endringsvedtak-PDF i amt-pdfgen")
}
