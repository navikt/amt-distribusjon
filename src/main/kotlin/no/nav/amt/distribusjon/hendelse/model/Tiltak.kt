package no.nav.amt.distribusjon.hendelse.model

import no.nav.amt.distribusjon.amtdeltaker.Tiltakstype

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

fun ArenaTiltakTypeKode.toTiltakskode() = when (this) {
    ArenaTiltakTypeKode.INDOPPFAG -> Tiltakstype.Tiltakskode.OPPFOLGING
    ArenaTiltakTypeKode.ARBFORB -> Tiltakstype.Tiltakskode.ARBEIDSFORBEREDENDE_TRENING
    ArenaTiltakTypeKode.AVKLARAG -> Tiltakstype.Tiltakskode.AVKLARING
    ArenaTiltakTypeKode.ARBRRHDAG -> Tiltakstype.Tiltakskode.ARBEIDSRETTET_REHABILITERING
    ArenaTiltakTypeKode.DIGIOPPARB -> Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK
    ArenaTiltakTypeKode.VASV -> Tiltakstype.Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET
    ArenaTiltakTypeKode.JOBBK -> Tiltakstype.Tiltakskode.JOBBKLUBB
    ArenaTiltakTypeKode.GRUPPEAMO -> Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING
    ArenaTiltakTypeKode.GRUFAGYRKE -> Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING
}
