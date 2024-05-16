package no.nav.amt.distribusjon.journalforing

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import java.util.UUID

class JournalforingstatusRepository {
    private fun rowmapper(row: Row) = Journalforingstatus(
        hendelseId = row.uuid("hendelse_id"),
        journalpostId = row.stringOrNull("journalpost_id"),
        skalSendeBrev = row.boolean("skal_sende_brev"),
        bestillingsId = row.uuidOrNull("bestillingsid"),
    )

    fun upsert(journalforingstatus: Journalforingstatus) = Database.query {
        val sql =
            """
            insert into journalforingstatus (hendelse_id, journalpost_id, skal_sende_brev, bestillingsid)
            values(:hendelse_id, :journalpost_id, :skal_sende_brev, :bestillingsid)
            on conflict (hendelse_id) do update set
                journalpost_id = :journalpost_id,
                skal_sende_brev = :skal_sende_brev,
                bestillingsid = :bestillingsid,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "hendelse_id" to journalforingstatus.hendelseId,
            "journalpost_id" to journalforingstatus.journalpostId,
            "skal_sende_brev" to journalforingstatus.skalSendeBrev,
            "bestillingsid" to journalforingstatus.bestillingsId,
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
