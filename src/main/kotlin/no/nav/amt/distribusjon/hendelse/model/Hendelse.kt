package no.nav.amt.distribusjon.hendelse.model

import java.time.LocalDateTime

data class Hendelse(
    val opprettet: LocalDateTime,
    val deltaker: HendelseDeltaker,
    val ansvarlig: HendelseAnsvarlig,
    val payload: HendelseType,
)
