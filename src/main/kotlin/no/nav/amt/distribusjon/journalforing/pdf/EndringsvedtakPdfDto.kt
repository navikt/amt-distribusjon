package no.nav.amt.distribusjon.journalforing.pdf

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

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
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EndringDto.EndreDeltakelsesmengde::class, name = "Deltakelsesmengde"),
    JsonSubTypes.Type(value = EndringDto.EndreStartdato::class, name = "Endre startdato"),
    JsonSubTypes.Type(value = EndringDto.EndreSluttdato::class, name = "Endre sluttdato"),
    JsonSubTypes.Type(value = EndringDto.ForlengDeltakelse::class, name = "Forlengelse"),
    JsonSubTypes.Type(value = EndringDto.IkkeAktuell::class, name = "Ikke aktuell"),
    JsonSubTypes.Type(value = EndringDto.AvsluttDeltakelse::class, name = "Avslutt deltakelse"),
)
sealed interface EndringDto {
    data class EndreDeltakelsesmengde(
        val deltakelsesprosent: Float?,
        val dagerPerUke: Float?,
    ) : EndringDto

    data class EndreStartdato(
        val startdato: LocalDate?,
        val sluttdato: LocalDate? = null,
    ) : EndringDto

    data class EndreSluttdato(
        val sluttdato: LocalDate,
    ) : EndringDto

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
    ) : EndringDto

    data class IkkeAktuell(
        val aarsak: String,
    ) : EndringDto

    data class AvsluttDeltakelse(
        val aarsak: String,
        val sluttdato: LocalDate,
    ) : EndringDto
}
