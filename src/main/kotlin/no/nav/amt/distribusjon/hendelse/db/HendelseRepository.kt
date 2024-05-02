package no.nav.amt.distribusjon.hendelse.db

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import java.time.LocalDateTime

class HendelseRepository {
    private fun rowmapper(row: Row) = HendelseDbo(
        id = row.uuid("id"),
        deltakerId = row.uuid("deltaker_id"),
        deltaker = objectMapper.readValue(row.string("deltaker")),
        ansvarlig = objectMapper.readValue(row.string("ansvarlig")),
        payload = objectMapper.readValue(row.string("payload")),
        opprettet = row.localDateTime("h.created_at"),
    )

    fun insert(hendelseDbo: HendelseDbo) = Database.query {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelseDbo.id,
            "deltaker_id" to hendelseDbo.deltakerId,
            "deltaker" to toPGObject(hendelseDbo.deltaker),
            "ansvarlig" to toPGObject(hendelseDbo.ansvarlig),
            "payload" to toPGObject(hendelseDbo.payload),
        )

        it.update(queryOf(sql, params))
    }

    fun getIkkeJournalforteHendelser(opprettet: LocalDateTime) = Database.query {
        val sql =
            """
            select h.id as "id",
            h.deltaker_id as "deltaker_id",
            h.deltaker as "deltaker",
            h.ansvarlig as "ansvarlig",
            h.payload as "payload",
            h.created_at as "h.created_at"
            from hendelse h
            left join journalforingstatus js on h.id = js.hendelse_id
            where h.created_at < :opprettet and js.hendelse_id is null
            """.trimIndent()

        val query = queryOf(sql, mapOf("opprettet" to opprettet))

        it.run(query.map(::rowmapper).asList)
    }
}
