package no.nav.amt.distribusjon.journalforing.pdf

data class HovedvedtakPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val navVeileder: NavVeilederDto,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val adresselinjer: List<String>,
        val innhold: List<String>,
        val bakgrunnsinformasjon: String?,
        val deltakelsesmengde: DeltakelsesmengdeDto?,
    )

    data class DeltakelsesmengdeDto(
        val deltakelsesprosent: Int?,
        val dagerPerUke: Int?,
    )

    data class DeltakerlisteDto(
        val navn: String,
        val ledetekst: String,
        val arrangor: ArrangorDto,
        val forskriftskapittel: Int,
    )

    data class ArrangorDto(val navn: String)

    data class NavVeilederDto(
        val navn: String,
        val enhet: String,
    )
}
