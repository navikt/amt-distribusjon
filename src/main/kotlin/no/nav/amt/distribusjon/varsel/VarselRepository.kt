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
        hendelser = row.array<UUID>("hendelser").toList(),
        aktivFra = row.zonedDateTime("aktiv_fra").withZoneSameInstant(ZoneId.of("Z")),
        aktivTil = row.zonedDateTimeOrNull("aktiv_til")?.withZoneSameInstant(ZoneId.of("Z")),
        deltakerId = row.uuid("deltaker_id"),
        personident = row.string("personident"),
        tekst = row.string("tekst"),
        skalVarsleEksternt = row.boolean("skal_varsle_eksternt"),
        erSendt = row.boolean("er_sendt"),
    )

    fun upsert(varsel: Varsel) = Database.query {
        val sql =
            """
            insert into varsel (id, type, hendelser, tekst, aktiv_fra, aktiv_til, deltaker_id, personident, skal_varsle_eksternt, er_sendt)
            values(:id, :type, :hendelser, :tekst, :aktiv_fra, :aktiv_til, :deltaker_id, :personident, :skal_varsle_eksternt, :er_sendt)
            on conflict (id) do update set
                type = :type,
                hendelser = :hendelser,
                tekst = :tekst,
                aktiv_fra = :aktiv_fra,
                aktiv_til = :aktiv_til,
                er_sendt = :er_sendt,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to varsel.id,
            "type" to varsel.type.name,
            "hendelser" to varsel.hendelser.toTypedArray(),
            "tekst" to varsel.tekst,
            "aktiv_fra" to varsel.aktivFra,
            "aktiv_til" to varsel.aktivTil,
            "deltaker_id" to varsel.deltakerId,
            "personident" to varsel.personident,
            "skal_varsle_eksternt" to varsel.skalVarsleEksternt,
            "er_sendt" to varsel.erSendt,
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

    fun getAktivt(deltakerId: UUID) = Database.query {
        val sql =
            """
            select * 
            from varsel
            where deltaker_id = :deltaker_id
                and er_sendt = true
                and (aktiv_til is null or aktiv_til > current_timestamp)
            """.trimIndent()
        it.run(queryOf(sql, mapOf("deltaker_id" to deltakerId)).map(::rowmapper).asSingle)?.let { varsel -> Result.success(varsel) }
            ?: Result.failure(NoSuchElementException())
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
            where :hendelse_id = any(hendelser)
            """.trimIndent()

        val query = queryOf(sql, mapOf("hendelse_id" to hendelseId))

        it.run(query.map(::rowmapper).asSingle)?.let { varsel ->
            Result.success(varsel)
        } ?: Result.failure(NoSuchElementException("Fant ikke varsel for hendelse $hendelseId"))
    }

    fun getIkkeSendt(deltakerId: UUID) = Database.query {
        val sql =
            """
            select * 
            from varsel
            where deltaker_id = :deltaker_id and er_sendt = false
            """.trimIndent()

        val query = queryOf(sql, mapOf("deltaker_id" to deltakerId))

        it.run(query.map(::rowmapper).asSingle)?.let { varsel ->
            Result.success(varsel)
        } ?: Result.failure(NoSuchElementException("Fant ikke varsel som ikke var sendt for deltaker $deltakerId"))
    }

    fun getVentende() = Database.query {
        val sql =
            """
            select * 
            from varsel
            where er_sendt = false and aktiv_fra at time zone 'UTC' < current_timestamp at time zone 'UTC'
            """.trimIndent()

        it.run(queryOf(sql).map(::rowmapper).asList)
    }
}
