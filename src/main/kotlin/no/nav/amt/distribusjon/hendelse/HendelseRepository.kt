package no.nav.amt.distribusjon.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import java.time.LocalDateTime

class HendelseRepository {
    private fun rowmapper(row: Row) = Hendelse(
        id = row.uuid("id"),
        deltaker = objectMapper.readValue(row.string("deltaker")),
        ansvarlig = objectMapper.readValue(row.string("ansvarlig")),
        payload = objectMapper.readValue(row.string("payload")),
        opprettet = row.localDateTime("h.created_at"),
        distribusjonskanal = row.string("distribusjonskanal").let { Distribusjonskanal.valueOf(it) },
    )

    fun insert(hendelse: Hendelse) = Database.query {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload, distribusjonskanal)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload, :distribusjonskanal)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelse.id,
            "deltaker_id" to hendelse.deltaker.id,
            "deltaker" to toPGObject(hendelse.deltaker),
            "ansvarlig" to toPGObject(hendelse.ansvarlig),
            "payload" to toPGObject(hendelse.payload),
            "distribusjonskanal" to hendelse.distribusjonskanal.name,
        )

        it.update(queryOf(sql, params))
    }

    fun getIkkeJournalforteHendelser(opprettet: LocalDateTime) = Database.query {
        val sql =
            """
            select h.id as "id",
            h.deltaker as "deltaker",
            h.ansvarlig as "ansvarlig",
            h.payload as "payload",
            h.created_at as "h.created_at",
            h.distribusjonskanal as "distribusjonskanal"
            from hendelse h
            left join journalforingstatus js on h.id = js.hendelse_id
            where h.created_at < :opprettet and js.hendelse_id is null
            """.trimIndent()

        val query = queryOf(sql, mapOf("opprettet" to opprettet))

        it.run(query.map(::rowmapper).asList)
    }
}
