package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.nowUTC
import java.time.ZonedDateTime
import java.util.UUID

object Varselsdata {
    fun varsel(
        type: Varsel.Type,
        id: UUID = UUID.randomUUID(),
        aktivFra: ZonedDateTime = nowUTC().plusHours(1),
        aktivTil: ZonedDateTime? = null,
        deltakerId: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        tekst: String = "Varselstekst",
    ) = Varsel(id, type, aktivFra, aktivTil, deltakerId, personident, tekst)
}