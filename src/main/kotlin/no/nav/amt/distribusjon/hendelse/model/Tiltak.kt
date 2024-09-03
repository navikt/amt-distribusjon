package no.nav.amt.distribusjon.hendelse.model

data class Tiltak(
    val navn: String,
    val type: Type,
    val ledetekst: String,
) {
    enum class Type {
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
}
