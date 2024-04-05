package no.nav.amt.distribusjon.hendelse.model

import java.util.UUID

data class HendelseDeltaker(
    val id: UUID,
    val personident: String,
    val deltakerliste: Deltakerliste,
) {
    data class Deltakerliste(
        val id: UUID,
        val navn: String,
        val arrangor: Arrangor,
        val tiltak: Tiltak,
    ) {
        data class Arrangor(
            val id: UUID,
            val organisasjonsnummer: String,
            val navn: String,
            val overordnetArrangor: Arrangor?,
        )

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
    }
}