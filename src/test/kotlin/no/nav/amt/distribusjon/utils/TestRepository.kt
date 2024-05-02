package no.nav.amt.distribusjon.utils

import kotliquery.queryOf
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import no.nav.amt.distribusjon.hendelse.db.HendelseDbo

object TestRepository {
    fun cleanDatabase() = Database.query { session ->
        val tables = listOf(
            "varsel",
            "journalforingstatus",
            "hendelse",
        )
        tables.forEach {
            val query = queryOf(
                """delete from $it""",
                emptyMap(),
            )

            session.update(query)
        }
    }

    fun insert(hendelseDbo: HendelseDbo) = Database.query {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload, created_at)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload, :created_at)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelseDbo.id,
            "deltaker_id" to hendelseDbo.deltakerId,
            "deltaker" to toPGObject(hendelseDbo.deltaker),
            "ansvarlig" to toPGObject(hendelseDbo.ansvarlig),
            "payload" to toPGObject(hendelseDbo.payload),
            "created_at" to hendelseDbo.opprettet,
        )

        it.update(queryOf(sql, params))
    }
}
