package no.nav.amt.distribusjon.journalforing

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.lib.utils.database.Database
import java.util.UUID

class JournalforingstatusRepository {
    private fun rowmapper(row: Row) = Journalforingstatus(
        hendelseId = row.uuid("hendelse_id"),
        journalpostId = row.stringOrNull("journalpost_id"),
        bestillingsId = row.uuidOrNull("bestillingsid"),
        kanIkkeDistribueres = row.boolean("kan_ikke_distribueres"),
    )

    fun upsert(journalforingstatus: Journalforingstatus) = Database.query {
        val sql =
            """
            insert into journalforingstatus (hendelse_id, journalpost_id, bestillingsid, kan_ikke_distribueres)
            values(:hendelse_id, :journalpost_id, :bestillingsid, :kan_ikke_distribueres)
            on conflict (hendelse_id) do update set
                journalpost_id = :journalpost_id,
                bestillingsid = :bestillingsid,
                kan_ikke_distribueres = :kan_ikke_distribueres,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "hendelse_id" to journalforingstatus.hendelseId,
            "journalpost_id" to journalforingstatus.journalpostId,
            "bestillingsid" to journalforingstatus.bestillingsId,
            "kan_ikke_distribueres" to journalforingstatus.kanIkkeDistribueres,
        )

        it.update(queryOf(sql, params))
    }

    fun get(hendelseId: UUID) = Database.query {
        val sql =
            """
            select * 
            from journalforingstatus
            where hendelse_id = :hendelse_id
            """.trimIndent()

        val query = queryOf(sql, mapOf("hendelse_id" to hendelseId))

        it.run(query.map(::rowmapper).asSingle)
    }
}
