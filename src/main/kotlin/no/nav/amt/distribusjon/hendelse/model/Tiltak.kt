package no.nav.amt.distribusjon.hendelse.model

data class Tiltak(
    val navn: String,
    val type: ArenaTiltakTypeKode,
    val ledetekst: String,
    val tiltakskode: Tiltakskode?,
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

enum class Tiltakskode {
    ARBEIDSFORBEREDENDE_TRENING,
    ARBEIDSRETTET_REHABILITERING,
    AVKLARING,
    DIGITALT_OPPFOLGINGSTILTAK,
    GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    GRUPPE_FAG_OG_YRKESOPPLAERING,
    JOBBKLUBB,
    OPPFOLGING,
    VARIG_TILRETTELAGT_ARBEID_SKJERMET,
}

fun ArenaTiltakTypeKode.toTiltakskode() = when (this) {
    ArenaTiltakTypeKode.INDOPPFAG -> Tiltakskode.OPPFOLGING
    ArenaTiltakTypeKode.ARBFORB -> Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
    ArenaTiltakTypeKode.AVKLARAG -> Tiltakskode.AVKLARING
    ArenaTiltakTypeKode.ARBRRHDAG -> Tiltakskode.ARBEIDSRETTET_REHABILITERING
    ArenaTiltakTypeKode.DIGIOPPARB -> Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK
    ArenaTiltakTypeKode.VASV -> Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET
    ArenaTiltakTypeKode.JOBBK -> Tiltakskode.JOBBKLUBB
    ArenaTiltakTypeKode.GRUPPEAMO -> Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING
    ArenaTiltakTypeKode.GRUFAGYRKE -> Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
}
