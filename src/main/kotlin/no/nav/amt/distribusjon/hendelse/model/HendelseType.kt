package no.nav.amt.distribusjon.hendelse.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = HendelseType.EndreStartdato::class, name = "EndreStartdato"),
    JsonSubTypes.Type(value = HendelseType.EndreSluttdato::class, name = "EndreSluttdato"),
    JsonSubTypes.Type(value = HendelseType.EndreDeltakelsesmengde::class, name = "EndreDeltakelsesmengde"),
    JsonSubTypes.Type(value = HendelseType.EndreBakgrunnsinformasjon::class, name = "EndreBakgrunnsinformasjon"),
    JsonSubTypes.Type(value = HendelseType.EndreInnhold::class, name = "EndreInnhold"),
    JsonSubTypes.Type(value = HendelseType.ForlengDeltakelse::class, name = "ForlengDeltakelse"),
    JsonSubTypes.Type(value = HendelseType.EndreSluttarsak::class, name = "EndreSluttarsak"),
    JsonSubTypes.Type(value = HendelseType.OpprettUtkast::class, name = "OpprettUtkast"),
    JsonSubTypes.Type(value = HendelseType.EndreUtkast::class, name = "EndreUtkast"),
    JsonSubTypes.Type(value = HendelseType.AvbrytUtkast::class, name = "AvbrytUtkast"),
    JsonSubTypes.Type(value = HendelseType.AvsluttDeltakelse::class, name = "AvsluttDeltakelse"),
    JsonSubTypes.Type(value = HendelseType.IkkeAktuell::class, name = "IkkeAktuell"),
    JsonSubTypes.Type(value = HendelseType.InnbyggerGodkjennUtkast::class, name = "InnbyggerGodkjennUtkast"),
    JsonSubTypes.Type(value = HendelseType.NavGodkjennUtkast::class, name = "NavGodkjennUtkast"),
)
sealed interface HendelseType {
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
    ) : HendelseType

    data class EndreStartdato(
        val startdato: LocalDate?,
        val sluttdato: LocalDate? = null,
    ) : HendelseType

    data class EndreSluttdato(
        val sluttdato: LocalDate,
    ) : HendelseType

    data class ForlengDeltakelse(
        val sluttdato: LocalDate,
    ) : HendelseType

    data class IkkeAktuell(
        val aarsak: Aarsak,
    ) : HendelseType

    data class AvsluttDeltakelse(
        val aarsak: Aarsak,
        val sluttdato: LocalDate,
    ) : HendelseType

    data class EndreSluttarsak(
        val aarsak: Aarsak,
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
            Type.SYK -> "syk"
            Type.FATT_JOBB -> "fått jobb"
            Type.TRENGER_ANNEN_STOTTE -> "trenger annen støtte"
            Type.FIKK_IKKE_PLASS -> "fikk ikke plass"
            Type.IKKE_MOTT -> "møter ikke opp"
            Type.ANNET -> "annet"
            Type.AVLYST_KONTRAKT -> "avlyst kontrakt"
            Type.UTDANNING -> "utdanning"
        }
    }
}

data class Innhold(
    val tekst: String,
    val innholdskode: String,
    val beskrivelse: String?,
) {
    fun visningsnavn(): String {
        return beskrivelse ?: tekst
    }
}
