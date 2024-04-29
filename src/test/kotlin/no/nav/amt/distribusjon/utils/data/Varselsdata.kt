package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.nowUTC
import java.time.ZonedDateTime
import java.util.UUID

object Varselsdata {
    fun varsel(
        type: Varsel.Type,
        id: UUID = UUID.randomUUID(),
        hendelseId: UUID = UUID.randomUUID(),
        aktivFra: ZonedDateTime = nowUTC(),
        aktivTil: ZonedDateTime? = null,
        deltakerId: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        tekst: String = "Varselstekst",
        skalVarsleEksternt: Boolean = if (type == Varsel.Type.OPPGAVE) true else false,
    ) = Varsel(id, type, hendelseId, aktivFra, aktivTil, deltakerId, personident, tekst, skalVarsleEksternt)
}
