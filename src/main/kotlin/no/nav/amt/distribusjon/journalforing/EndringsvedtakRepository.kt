package no.nav.amt.distribusjon.journalforing

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import no.nav.amt.distribusjon.journalforing.model.Endringsvedtak
import java.time.LocalDateTime
import java.util.UUID

class EndringsvedtakRepository {
    private fun rowmapper(row: Row) = Endringsvedtak(
        hendelseId = row.uuid("hendelse_id"),
        deltakerId = row.uuid("deltaker_id"),
        hendelse = objectMapper.readValue(row.string("hendelse")),
        opprettet = row.localDateTime("created_at"),
    )

    fun insert(endringsvedtak: Endringsvedtak) = Database.query {
        val sql =
            """
            insert into endringsvedtak (hendelse_id, deltaker_id, hendelse)
            values(:hendelse_id, :deltaker_id, :hendelse)
            on conflict (hendelse_id) do nothing
            """.trimIndent()

        val params = mapOf(
            "hendelse_id" to endringsvedtak.hendelseId,
            "deltaker_id" to endringsvedtak.deltakerId,
            "hendelse" to toPGObject(endringsvedtak.hendelse),
        )

        it.update(queryOf(sql, params))
    }

    fun getEndringsvedtak(opprettet: LocalDateTime) = Database.query {
        val sql =
            """
            select * 
            from endringsvedtak
            where created_at < :opprettet
            """.trimIndent()

        val query = queryOf(sql, mapOf("opprettet" to opprettet))

        it.run(query.map(::rowmapper).asList)
    }

    fun deleteEndringsvedtak(hendelseIder: List<UUID>) = Database.query {
        if (hendelseIder.isEmpty()) {
            return@query
        }
        val sql =
            """
            delete from endringsvedtak
            where hendelse_id in (${hendelseIder.joinToString { "?" }})
            """.trimIndent()

        val query = queryOf(
            sql,
            *hendelseIder.toTypedArray(),
        )

        it.update(query)
    }
}
