package no.nav.amt.distribusjon.journalforing.pdf

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

    data class ArrangorDto(
        val navn: String,
    )

    data class NavVeilederDto(
        val navn: String,
        val enhet: String,
    )
}

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface EndringDto {
    data class EndreDeltakelsesmengde(
        val deltakelsesprosent: Int?,
        val dagerPerUkeTekst: String?,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class EndreStartdato(
        val startdato: LocalDate?,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class EndreStartdatoOgVarighet(
        val startdato: LocalDate?,
        val sluttdato: LocalDate,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class EndreSluttdato(
        val sluttdato: LocalDate,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class IkkeAktuell(
        val aarsak: String,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class AvsluttDeltakelse(
        val aarsak: String,
        val sluttdato: LocalDate,
        val begrunnelseFraNav: String?,
        val begrunnelseFraArrangor: String?,
    ) : EndringDto

    data class EndreBakgrunnsinformasjon(
        val bakgrunnsinformasjon: String?,
    ) : EndringDto

    data class EndreInnhold(
        val innhold: List<String>,
    ) : EndringDto

    data class LeggTilOppstartsdato(
        val startdatoFraArrangor: LocalDate,
        val sluttdatoFraArrangor: LocalDate?,
    ) : EndringDto
}
