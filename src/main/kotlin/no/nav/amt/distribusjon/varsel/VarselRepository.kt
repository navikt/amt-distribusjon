package no.nav.amt.distribusjon.varsel

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.varsel.model.Varsel
import java.util.NoSuchElementException
import java.util.UUID

class VarselRepository {
    fun rowmapper(row: Row) = Varsel(
        id = row.uuid("id"),
        type = Varsel.Type.valueOf(row.string("type")),
        aktivFra = row.zonedDateTime("aktiv_fra"),
        aktivTil = row.zonedDateTimeOrNull("aktiv_til"),
        deltakerId = row.uuid("deltaker_id"),
        personident = row.string("personident"),
        tekst = row.string("tekst"),
    )

    fun upsert(varsel: Varsel) = Database.query {
        val sql =
            """
            insert into varsel (id, type, aktiv_fra, aktiv_til, deltaker_id, personident)
            values(:id, :type, :aktiv_fra, :aktiv_til, :deltaker_id, personident)
            on conflict (id) do update set
                aktiv_fra = :aktiv_fra,
                aktiv_til = :aktiv_til
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to varsel.id,
            "type" to varsel.type,
            "aktiv_fra" to varsel.aktivFra,
            "aktiv_til" to varsel.aktivTil,
            "deltaker_id" to varsel.deltakerId,
            "personident" to varsel.personident,
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
        } ?: Result.failure(NoSuchElementException(""))
    }
}
