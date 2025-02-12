package no.nav.amt.distribusjon.hendelse.model

import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.models.hendelse.HendelseDeltaker

fun HendelseDeltaker.Deltakerliste.deltakerAdresseDeles() = !tiltakUtenDeltakerAdresse.contains(this.tiltak.tiltakskode)

private val tiltakUtenDeltakerAdresse = setOf(
    Tiltakstype.Tiltakskode.DIGITALT_OPPFOLGINGSTILTAK,
    Tiltakstype.Tiltakskode.JOBBKLUBB,
    Tiltakstype.Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING,
    Tiltakstype.Tiltakskode.GRUPPE_FAG_OG_YRKESOPPLAERING,
)
