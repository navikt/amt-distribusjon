package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype

data class InnsokingsbrevPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val avsender: AvsenderDto,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personident: String,
    )

    data class DeltakerlisteDto(
        val navn: String,
        val tiltakskode: Tiltakstype.Tiltakskode,
        val arrangor: ArrangorDto,
        val startdato: String,
        val sluttdato: String?,
    )
}
