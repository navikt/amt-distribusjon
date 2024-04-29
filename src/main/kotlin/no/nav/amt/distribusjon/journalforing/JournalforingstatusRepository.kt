package no.nav.amt.distribusjon.journalforing

import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import java.util.UUID

class JournalforingstatusRepository {
    private fun rowmapper(row: Row) = Journalforingstatus(
        hendelseId = row.uuid("hendelse_id"),
        journalpostId = row.string("journalpost_id"),
    )

    fun insert(journalforingstatus: Journalforingstatus) = Database.query {
        val sql =
            """
            insert into journalforingstatus (hendelse_id, journalpost_id)
            values(:hendelse_id, :journalpost_id)
            """.trimIndent()

        val params = mapOf(
            "hendelse_id" to journalforingstatus.hendelseId,
            "journalpost_id" to journalforingstatus.journalpostId,
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
