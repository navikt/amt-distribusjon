package no.nav.amt.distribusjon.hendelse.model

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakskode
import no.nav.amt.lib.models.hendelse.HendelseDeltaker

fun HendelseDeltaker.Deltakerliste.deltakerAdresseDeles() = !tiltakUtenDeltakerAdresse.contains(this.tiltak.tiltakskode)

private val tiltakUtenDeltakerAdresse = setOf(
    Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
    Tiltakskode.JOBBKLUBB,
    Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
)
