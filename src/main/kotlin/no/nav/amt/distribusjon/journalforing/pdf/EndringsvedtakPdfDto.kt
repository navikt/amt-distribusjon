package no.nav.amt.distribusjon.journalforing.pdf

import no.nav.amt.distribusjon.hendelse.model.HendelseType

data class EndringsvedtakPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val endringer: List<EndringDto>,
    val navVeileder: NavVeilederDto,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val adresselinjer: List<String>,
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

    data class EndringDto(
        val navn: String,
        val hendelseType: HendelseType,
    )
}
