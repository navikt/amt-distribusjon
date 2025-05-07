package no.nav.amt.distribusjon.journalforing.pdf

import java.time.LocalDate

data class VentelistebrevPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val avsender: AvsenderDto,
    val opprettetDato: LocalDate,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personident: String,
        val opprettetDato: LocalDate,
    )

    data class DeltakerlisteDto(
        val ingressNavn: String,
        val tittelNavn: String,
        val arrangor: ArrangorDto,
        val startdato: LocalDate?,
    )
}
