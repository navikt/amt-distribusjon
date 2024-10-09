package no.nav.amt.distribusjon.hendelse.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.amt.lib.models.arrangor.melding.Forslag
import java.time.LocalDate
import java.time.ZonedDateTime

@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface HendelseType {
    sealed interface HendelseMedForslag : HendelseType {
        val begrunnelseFraNav: String?
        val begrunnelseFraArrangor: String?
        val endringFraForslag: Forslag.Endring?
    }

    data class OpprettUtkast(
        val utkast: Utkast,
    ) : HendelseType

    data class EndreUtkast(
        val utkast: Utkast,
    ) : HendelseType

    data class AvbrytUtkast(
        val utkast: Utkast,
    ) : HendelseType

    data class InnbyggerGodkjennUtkast(
        val utkast: Utkast,
    ) : HendelseType

    data class NavGodkjennUtkast(
        val utkast: Utkast,
    ) : HendelseType

    data class EndreBakgrunnsinformasjon(
        val bakgrunnsinformasjon: String?,
    ) : HendelseType

    data class EndreInnhold(
        val innhold: List<Innhold>,
    ) : HendelseType

    data class EndreDeltakelsesmengde(
        val deltakelsesprosent: Float?,
        val dagerPerUke: Float?,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreStartdato(
        val startdato: LocalDate?,
        val sluttdato: LocalDate? = null,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreSluttdato(
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class IkkeAktuell(
        val aarsak: Aarsak,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class AvsluttDeltakelse(
        val aarsak: Aarsak,
        val sluttdato: LocalDate,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class EndreSluttarsak(
        val aarsak: Aarsak,
        override val begrunnelseFraNav: String?,
        override val begrunnelseFraArrangor: String?,
        override val endringFraForslag: Forslag.Endring?,
    ) : HendelseMedForslag

    data class DeltakerSistBesokt(
        val sistBesokt: ZonedDateTime,
    ) : HendelseType

    data class ReaktiverDeltakelse(
        val utkast: Utkast,
        val begrunnelseFraNav: String,
    ) : HendelseType

    data class LeggTilOppstartsdato(
        val startdato: LocalDate,
        val sluttdato: LocalDate?,
    ) : HendelseType
}

data class Utkast(
    val startdato: LocalDate?,
    val sluttdato: LocalDate?,
    val dagerPerUke: Float?,
    val deltakelsesprosent: Float?,
    val bakgrunnsinformasjon: String?,
    val innhold: List<Innhold>,
)

data class Aarsak(
    val type: Type,
    val beskrivelse: String? = null,
) {
    init {
        if (beskrivelse != null && type != Type.ANNET) {
            error("Aarsak $type skal ikke ha beskrivelse")
        }
    }

    enum class Type {
        SYK,
        FATT_JOBB,
        TRENGER_ANNEN_STOTTE,
        FIKK_IKKE_PLASS,
        UTDANNING,
        IKKE_MOTT,
        AVLYST_KONTRAKT,
        ANNET,
    }

    fun visningsnavn(): String {
        if (beskrivelse != null) {
            return beskrivelse
        }
        return when (type) {
            Type.SYK -> "Syk"
            Type.FATT_JOBB -> "Fått jobb"
            Type.TRENGER_ANNEN_STOTTE -> "Trenger annen støtte"
            Type.FIKK_IKKE_PLASS -> "Fikk ikke plass"
            Type.IKKE_MOTT -> "Møter ikke opp"
            Type.ANNET -> "Annet"
            Type.AVLYST_KONTRAKT -> "Avlyst kontrakt"
            Type.UTDANNING -> "Utdanning"
        }
    }
}

data class Innhold(
    val tekst: String,
    val innholdskode: String,
    val beskrivelse: String?,
) {
    fun visningsnavn(): String = beskrivelse ?: tekst
}
