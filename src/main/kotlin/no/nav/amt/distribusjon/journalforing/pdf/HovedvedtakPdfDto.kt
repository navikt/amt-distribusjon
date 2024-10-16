package no.nav.amt.distribusjon.journalforing.pdf

import java.time.LocalDate

data class HovedvedtakPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val avsender: AvsenderDto,
    val vedtaksdato: LocalDate,
    val begrunnelseFraNav: String? = null,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personident: String,
        val innhold: List<String>,
        val bakgrunnsinformasjon: String?,
        val deltakelsesmengdeTekst: String?,
        val adresseDelesMedArrangor: Boolean,
    )

    data class DeltakerlisteDto(
        val navn: String,
        val ledetekst: String,
        val arrangor: ArrangorDto,
        val forskriftskapittel: Int,
    )

    data class ArrangorDto(val navn: String)

    data class AvsenderDto(
        val navn: String,
        val enhet: String,
    )
}
