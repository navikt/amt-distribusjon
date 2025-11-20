package no.nav.amt.distribusjon.tiltakshendelse.model

import no.nav.amt.lib.models.deltakerliste.tiltakstype.ArenaKode
import java.time.LocalDateTime
import java.util.UUID

data class TiltakshendelseDto(
    val id: UUID,
    val fnr: String,
    val aktiv: Boolean,
    val tekst: String,
    val lenke: String,
    val tiltakstype: ArenaKode, // kan ikke endres, publiseres på obo.tiltakshendelse-v1
    val opprettet: LocalDateTime,
    val avsender: String = "KOMET",
)

fun Tiltakshendelse.toDto() = TiltakshendelseDto(
    id = id,
    fnr = personident,
    aktiv = aktiv,
    tekst = tekst,
    lenke = lenke,
    tiltakstype = tiltakskode.toArenaKode(),
    opprettet = opprettet,
)
