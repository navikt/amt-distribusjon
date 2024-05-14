package no.nav.amt.distribusjon.journalforing.pdf

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

data class EndringsvedtakPdfDto(
    val deltaker: DeltakerDto,
    val deltakerliste: DeltakerlisteDto,
    val endringer: List<EndringDto>,
    val navVeileder: NavVeilederDto,
    val vedtaksdato: LocalDate,
    val forsteVedtakFattet: LocalDate,
) {
    data class DeltakerDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val personident: String,
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
    JsonSubTypes.Type(value = EndringDto.EndreStartdato::class, name = "Oppstartsdato"),
    JsonSubTypes.Type(value = EndringDto.EndreStartdatoOgVarighet::class, name = "Oppstartsdato og varighet"),
    JsonSubTypes.Type(value = EndringDto.EndreSluttdato::class, name = "Sluttdato"),
    JsonSubTypes.Type(value = EndringDto.ForlengDeltakelse::class, name = "Forlengelse"),
    JsonSubTypes.Type(value = EndringDto.IkkeAktuell::class, name = "Er ikke aktuell"),
    JsonSubTypes.Type(value = EndringDto.AvsluttDeltakelse::class, name = "Avslutt deltakelse"),
    JsonSubTypes.Type(value = EndringDto.EndreInnhold::class, name = "Innhold"),
    JsonSubTypes.Type(value = EndringDto.EndreBakgrunnsinformasjon::class, name = "Bakgrunnsinfo"),
)
sealed interface EndringDto {
    data class EndreDeltakelsesmengde(
        val deltakelsesmengdeTekst: String,
    ) : EndringDto

    data class EndreStartdato(
        val startdato: LocalDate?,
    ) : EndringDto

    data class EndreStartdatoOgVarighet(
        val startdato: LocalDate?,
        val sluttdato: LocalDate,
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

    data class EndreBakgrunnsinformasjon(
        val bakgrunnsinformasjon: String?,
    ) : EndringDto

    data class EndreInnhold(
        val innhold: List<String>,
    ) : EndringDto
}
