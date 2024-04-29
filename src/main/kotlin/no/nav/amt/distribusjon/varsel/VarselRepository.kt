package no.nav.amt.distribusjon.varsel

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.varsel.model.Varsel
import java.time.ZoneId
import java.util.NoSuchElementException
import java.util.UUID

class VarselRepository {
    fun rowmapper(row: Row) = Varsel(
        id = row.uuid("id"),
        type = Varsel.Type.valueOf(row.string("type")),
        hendelseId = row.uuid("hendelse_id"),
        aktivFra = row.zonedDateTime("aktiv_fra").withZoneSameInstant(ZoneId.of("Z")),
        aktivTil = row.zonedDateTimeOrNull("aktiv_til")?.withZoneSameInstant(ZoneId.of("Z")),
        deltakerId = row.uuid("deltaker_id"),
        personident = row.string("personident"),
        tekst = row.string("tekst"),
        skalVarsleEksternt = row.boolean("skal_varsle_eksternt"),
    )

    fun upsert(varsel: Varsel) = Database.query {
        val sql =
            """
            insert into varsel (id, type, hendelse_id, tekst, aktiv_fra, aktiv_til, deltaker_id, personident, skal_varsle_eksternt)
            values(:id, :type, :hendelse_id, :tekst, :aktiv_fra, :aktiv_til, :deltaker_id, :personident, :skal_varsle_eksternt)
            on conflict (id) do update set
                aktiv_fra = :aktiv_fra,
                aktiv_til = :aktiv_til,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to varsel.id,
            "type" to varsel.type.name,
            "hendelse_id" to varsel.hendelseId,
            "tekst" to varsel.tekst,
            "aktiv_fra" to varsel.aktivFra,
            "aktiv_til" to varsel.aktivTil,
            "deltaker_id" to varsel.deltakerId,
            "personident" to varsel.personident,
            "skal_varsle_eksternt" to varsel.skalVarsleEksternt,
        )

        it.update(queryOf(sql, params))
    }

    fun getSisteVarsel(deltakerId: UUID, type: Varsel.Type) = Database.query {
        val sql =
            """
            select * 
            from varsel
            where deltaker_id = :deltaker_id and type = :type
            order by aktiv_fra desc
            limit 1
            """.trimIndent()

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId, "type" to type.name))

        it.run(query.map(::rowmapper).asSingle)?.let { varsel ->
            Result.success(varsel)
        } ?: Result.failure(NoSuchElementException("Fant ingen varsel av type $type for deltaker $deltakerId"))
    }

    fun get(id: UUID) = Database.query {
        val sql =
            """
            select * 
            from varsel
            where id = :id
            """.trimIndent()

        val query = queryOf(sql, mapOf("id" to id))

        it.run(query.map(::rowmapper).asSingle)?.let { varsel ->
            Result.success(varsel)
        } ?: Result.failure(NoSuchElementException("Fant ikke varsel $id"))
    }

    fun getByHendelseId(hendelseId: UUID) = Database.query {
        val sql =
            """
            select * 
            from varsel
            where hendelse_id = :hendelse_id
            """.trimIndent()

        val query = queryOf(sql, mapOf("hendelse_id" to hendelseId))

        it.run(query.map(::rowmapper).asSingle)?.let { varsel ->
            Result.success(varsel)
        } ?: Result.failure(NoSuchElementException("Fant ikke varsel for hendelse $hendelseId"))
    }
}
