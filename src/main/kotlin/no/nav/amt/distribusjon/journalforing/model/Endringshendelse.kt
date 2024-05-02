package no.nav.amt.distribusjon.journalforing.model

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import java.time.LocalDateTime
import java.util.UUID

data class Endringshendelse(
    val hendelseId: UUID,
    val deltakerId: UUID,
    val hendelse: Hendelse,
    val opprettet: LocalDateTime,
)
