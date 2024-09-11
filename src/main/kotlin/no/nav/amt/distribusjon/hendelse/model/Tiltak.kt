package no.nav.amt.distribusjon.hendelse.model

data class Tiltak(
    val navn: String,
    val type: ArenaTiltakTypeKode,
    val ledetekst: String,
)

enum class ArenaTiltakTypeKode {
    INDOPPFAG,
    ARBFORB,
    AVKLARAG,
    VASV,
    ARBRRHDAG,
    DIGIOPPARB,
    JOBBK,
    GRUPPEAMO,
    GRUFAGYRKE,
}
