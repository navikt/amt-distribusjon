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
        status = row.string("status").let { Varsel.Status.valueOf(it) },
        aktivFra = row.zonedDateTime("aktiv_fra").withZoneSameInstant(ZoneId.of("Z")),
        aktivTil = row.zonedDateTimeOrNull("aktiv_til")?.withZoneSameInstant(ZoneId.of("Z")),
        deltakerId = row.uuid("deltaker_id"),
        personident = row.string("personident"),
        tekst = row.string("tekst"),
        erEksterntVarsel = row.boolean("skal_varsle_eksternt"),
        sendt = row.zonedDateTimeOrNull("sendt")?.withZoneSameInstant(ZoneId.of("Z")),
        revarselForVarsel = row.uuidOrNull("revarsel_for_varsel"),
    )

    fun upsert(varsel: Varsel) = Database.query {
        val sql =
            """
            insert into varsel (
                id, 
                type, 
                hendelser, 
                status, 
                tekst, 
                aktiv_fra, 
                aktiv_til, 
                deltaker_id, 
                personident, 
                skal_varsle_eksternt, 
                er_sendt,
                sendt,
                skal_revarsles,
                revarsel_for_varsel
            )
            values(
                :id, 
                :type, 
                :hendelser, 
                :status, 
                :tekst, 
                :aktiv_fra, 
                :aktiv_til, 
                :deltaker_id, 
                :personident, 
                :skal_varsle_eksternt, 
                :er_sendt,
                :sendt,
                :skal_revarsles,
                :revarsel_for_varsel
            )
            on conflict (id) do update set
                type = :type,
                hendelser = :hendelser,
                tekst = :tekst,
                aktiv_fra = :aktiv_fra,
                aktiv_til = :aktiv_til,
                er_sendt = :er_sendt,
                status = :status,
                sendt = :sendt,
                skal_revarsles = :skal_revarsles,
                revarsel_for_varsel = :revarsel_for_varsel,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to varsel.id,
            "type" to varsel.type.name,
            "status" to varsel.status.name,
            "hendelser" to varsel.hendelser.toTypedArray(),
            "tekst" to varsel.tekst,
            "aktiv_fra" to varsel.aktivFra,
            "aktiv_til" to varsel.aktivTil,
            "deltaker_id" to varsel.deltakerId,
            "personident" to varsel.personident,
            "skal_varsle_eksternt" to varsel.erEksterntVarsel,
            "er_sendt" to varsel.erSendt,
            "sendt" to varsel.sendt,
            "skal_revarsles" to varsel.skalRevarsles,
            "revarsel_for_varsel" to varsel.revarselForVarsel,
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

    fun getVarslerSomSkalRevarsles() = Database.query {
        val sql =
            """
            select * 
            from varsel
            where status = 'AKTIV'
                and (aktiv_til is null or aktiv_til > current_timestamp)
                and aktiv_fra at time zone 'UTC' < current_timestamp at time zone 'UTC' - interval '40 hours'
            """.trimIndent()
        it.run(queryOf(sql).map(::rowmapper).asList)
    }

    fun stoppRevarsler(deltakerId: UUID) = Database.query {
        val sql =
            """
            update varsel
            set skal_revarsles = false
            where deltaker_id = ? and skal_revarsles = true -- and varsel_id != ?
            """.trimIndent()

        it.update(queryOf(sql, deltakerId))
    }
}
