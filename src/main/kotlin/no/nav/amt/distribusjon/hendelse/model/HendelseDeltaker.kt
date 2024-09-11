package no.nav.amt.distribusjon.hendelse.model

import java.time.LocalDate
import java.util.UUID

data class HendelseDeltaker(
    val id: UUID,
    val personident: String,
    val deltakerliste: Deltakerliste,
    val forsteVedtakFattet: LocalDate?,
) {
    data class Deltakerliste(
        val id: UUID,
        val navn: String,
        val arrangor: Arrangor,
        val tiltak: Tiltak,
        val erKurs: Boolean = false,
    ) {
        data class Arrangor(
            val id: UUID,
            val organisasjonsnummer: String,
            val navn: String,
            val overordnetArrangor: Arrangor?,
        )

        fun deltakerAdresseDeles() = !tiltakUtenDeltakerAdresse.contains(this.tiltak.type)
    }
}

private val tiltakUtenDeltakerAdresse = setOf(
    ArenaTiltakTypeKode.DIGIOPPARB,
    ArenaTiltakTypeKode.JOBBK,
    ArenaTiltakTypeKode.GRUPPEAMO,
    ArenaTiltakTypeKode.GRUFAGYRKE,
)
