package no.nav.amt.distribusjon.tiltakshendelse.model

import no.nav.amt.distribusjon.hendelse.model.Tiltak
import java.time.LocalDateTime
import java.util.UUID

data class TiltakshendelseDto(
    val id: UUID,
    val fnr: String,
    val aktiv: Boolean,
    val tekst: String,
    val lenke: String,
    val tiltakstype: Tiltak.Type,
    val opprettet: LocalDateTime,
    val avsender: String = "KOMET",
)

fun Tiltakshendelse.toDto() = TiltakshendelseDto(
    id = id,
    fnr = personident,
    aktiv = aktiv,
    tekst = tekst,
    lenke = lenke,
    tiltakstype = tiltakstype,
    opprettet = opprettet,
)
