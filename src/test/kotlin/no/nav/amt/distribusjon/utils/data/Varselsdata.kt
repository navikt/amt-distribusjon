package no.nav.amt.distribusjon.utils.data

import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.nowUTC
import java.time.ZonedDateTime
import java.util.UUID

object Varselsdata {
    fun varsel(
        type: Varsel.Type,
        status: Varsel.Status = Varsel.Status.VENTER_PA_UTSENDELSE,
        id: UUID = UUID.randomUUID(),
        hendelser: List<UUID> = listOf(UUID.randomUUID()),
        aktivFra: ZonedDateTime = nowUTC(),
        aktivTil: ZonedDateTime? = null,
        deltakerId: UUID = UUID.randomUUID(),
        personident: String = randomIdent(),
        tekst: String = "Varselstekst",
        erEksterntVarsel: Boolean = type == Varsel.Type.OPPGAVE,
        sendt: ZonedDateTime? = null,
        revarselForVarsel: UUID? = null,
    ) = Varsel(
        id,
        type,
        hendelser,
        status,
        erEksterntVarsel,
        revarselForVarsel,
        aktivFra,
        aktivTil,
        deltakerId,
        personident,
        tekst,
        sendt,
    )
}
