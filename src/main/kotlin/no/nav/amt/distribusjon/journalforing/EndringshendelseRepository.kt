package no.nav.amt.distribusjon.journalforing

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import no.nav.amt.distribusjon.journalforing.model.Endringshendelse
import java.time.LocalDateTime
import java.util.UUID

class EndringshendelseRepository {
    private fun rowmapper(row: Row) = Endringshendelse(
        hendelseId = row.uuid("hendelse_id"),
        deltakerId = row.uuid("deltaker_id"),
        hendelse = objectMapper.readValue(row.string("hendelse")),
        opprettet = row.localDateTime("created_at"),
    )

    fun insert(endringshendelse: Endringshendelse) = Database.query {
        val sql =
            """
            insert into endringshendelse (hendelse_id, deltaker_id, hendelse)
            values(:hendelse_id, :deltaker_id, :hendelse)
            on conflict (hendelse_id) do nothing
            """.trimIndent()

        val params = mapOf(
            "hendelse_id" to endringshendelse.hendelseId,
            "deltaker_id" to endringshendelse.deltakerId,
            "hendelse" to toPGObject(endringshendelse.hendelse),
        )

        it.update(queryOf(sql, params))
    }

    fun getHendelser(opprettet: LocalDateTime) = Database.query {
        val sql =
            """
            select * 
            from endringshendelse
            where created_at < :opprettet
            """.trimIndent()

        val query = queryOf(sql, mapOf("opprettet" to opprettet))

        it.run(query.map(::rowmapper).asList)
    }

    fun deleteHendelser(hendelseIder: List<UUID>) = Database.query {
        if (hendelseIder.isEmpty()) {
            return@query
        }
        val sql =
            """
            delete from endringshendelse
            where hendelse_id in (${hendelseIder.joinToString { "?" }})
            """.trimIndent()

        val query = queryOf(
            sql,
            *hendelseIder.toTypedArray(),
        )

        it.update(query)
    }
}
