package no.nav.amt.distribusjon.tiltakshendelse

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.lib.models.deltakerliste.tiltakstype.Tiltakstype
import no.nav.amt.lib.utils.database.Database
import java.util.UUID

class TiltakshendelseRepository {
    fun rowmapper(row: Row) = Tiltakshendelse(
        id = row.uuid("id"),
        type = row.string("type").let { Tiltakshendelse.Type.valueOf(it) },
        deltakerId = row.uuid("deltaker_id"),
        forslagId = row.uuidOrNull("forslag_id"),
        hendelser = row.array<UUID>("hendelser").toList(),
        personident = row.string("personident"),
        aktiv = row.boolean("aktiv"),
        tekst = row.string("tekst"),
        tiltakstype = row.string("tiltakstype").let { Tiltakstype.ArenaKode.valueOf(it) },
        opprettet = row.localDateTime("created_at"),
    )

    fun upsert(tiltakshendelse: Tiltakshendelse) = Database.query {
        val sql =
            """
            insert into tiltakshendelse (
                id, 
                type, 
                deltaker_id, 
                forslag_id, 
                hendelser, 
                personident, 
                aktiv, 
                tekst, 
                tiltakstype
            )
            values (
                :id, 
                :type, 
                :deltaker_id, 
                :forslag_id, 
                :hendelser, 
                :personident, 
                :aktiv, 
                :tekst, 
                :tiltakstype
            )
            on conflict (id) do update set
                hendelser = :hendelser,
                personident = :personident,
                aktiv = :aktiv,
                tekst = :tekst,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "id" to tiltakshendelse.id,
            "type" to tiltakshendelse.type.name,
            "deltaker_id" to tiltakshendelse.deltakerId,
            "forslag_id" to tiltakshendelse.forslagId,
            "hendelser" to tiltakshendelse.hendelser.toTypedArray(),
            "personident" to tiltakshendelse.personident,
            "aktiv" to tiltakshendelse.aktiv,
            "tekst" to tiltakshendelse.tekst,
            "tiltakstype" to tiltakshendelse.tiltakstype.name,
        )

        it.update(queryOf(sql, params))
    }

    fun get(id: UUID): Result<Tiltakshendelse> = Database.query {
        val sql =
            """
            select * 
            from tiltakshendelse
            where id = :id
            """.trimIndent()

        val query = queryOf(sql, mapOf("id" to id))

        it.run(query.map(::rowmapper).asSingle)?.let { tiltakshendelse ->
            Result.success(tiltakshendelse)
        } ?: Result.failure(NoSuchElementException("Fant ikke tiltakshendelse $id"))
    }

    fun getHendelse(deltakerId: UUID, hendelseType: Tiltakshendelse.Type) = Database.query {
        val sql =
            """
            select * 
            from tiltakshendelse
            where deltaker_id = :deltaker_id and type = :type
            """.trimIndent()

        val params = mapOf(
            "deltaker_id" to deltakerId,
            "type" to hendelseType.name,
        )

        it.run(queryOf(sql, params).map(::rowmapper).asSingle)?.let { tiltakshendelse ->
            Result.success(tiltakshendelse)
        } ?: Result.failure(NoSuchElementException("Fant ikke tiltakshendelse for deltaker $deltakerId"))
    }

    fun getForslagHendelse(forslagId: UUID) = Database.query {
        val sql =
            """
            select * 
            from tiltakshendelse
            where forslag_id = :forslag_id
            """.trimIndent()

        val params = mapOf("forslag_id" to forslagId)

        it.run(queryOf(sql, params).map(::rowmapper).asSingle)?.let { tiltakshendelse ->
            Result.success(tiltakshendelse)
        } ?: Result.failure(NoSuchElementException("Fant ikke tiltakshendelse for med forslagId $forslagId"))
    }

    fun getByHendelseId(hendelseId: UUID) = Database.query {
        val sql =
            """
            select * 
            from tiltakshendelse
            where :hendelse_id = any(hendelser)
            """.trimIndent()

        val query = queryOf(sql, mapOf("hendelse_id" to hendelseId))

        it.run(query.map(::rowmapper).asSingle)?.let { varsel ->
            Result.success(varsel)
        } ?: Result.failure(java.util.NoSuchElementException("Fant ikke tiltakshendelse for hendelse $hendelseId"))
    }

    fun getFeilProduserteHendelser() = Database.query {
        val sql =
            """
            select t.* from tiltakshendelse join outbox_record o on t.id = o.key::uuid where o.value_type = 'Tiltakshendelse';
            """.trimIndent()
        it.list(queryOf(sql), ::rowmapper)
    }
}
