package no.nav.amt.distribusjon.utils

import kotliquery.queryOf
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import no.nav.amt.distribusjon.hendelse.model.Hendelse

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

    fun insert(hendelse: Hendelse) = Database.query {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload, distribusjonskanal, created_at)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload, :distribusjonskanal, :created_at)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelse.id,
            "deltaker_id" to hendelse.deltaker.id,
            "deltaker" to toPGObject(hendelse.deltaker),
            "ansvarlig" to toPGObject(hendelse.ansvarlig),
            "payload" to toPGObject(hendelse.payload),
            "created_at" to hendelse.opprettet,
            "distribusjonskanal" to hendelse.distribusjonskanal.name,
        )

        it.update(queryOf(sql, params))
    }
}
