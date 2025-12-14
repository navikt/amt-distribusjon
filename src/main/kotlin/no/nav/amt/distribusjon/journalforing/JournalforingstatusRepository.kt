package no.nav.amt.distribusjon.journalforing

import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalforingstatusRepository(
    private val template: NamedParameterJdbcTemplate,
) {
    fun upsert(journalforingstatus: Journalforingstatus): Int {
        val sql =
            """
            insert into journalforingstatus (hendelse_id, journalpost_id, bestillingsid, kan_ikke_distribueres, kan_ikke_journalfores)
            values(:hendelse_id, :journalpost_id, :bestillingsid, :kan_ikke_distribueres, :kan_ikke_journalfores)
            on conflict (hendelse_id) do update set
                journalpost_id = :journalpost_id,
                bestillingsid = :bestillingsid,
                kan_ikke_distribueres = :kan_ikke_distribueres,
                kan_ikke_journalfores = :kan_ikke_journalfores,
                modified_at = current_timestamp
            """.trimIndent()

        val params = mapOf(
            "hendelse_id" to journalforingstatus.hendelseId,
            "journalpost_id" to journalforingstatus.journalpostId,
            "bestillingsid" to journalforingstatus.bestillingsId,
            "kan_ikke_distribueres" to journalforingstatus.kanIkkeDistribueres,
            "kan_ikke_journalfores" to journalforingstatus.kanIkkeJournalfores,
        )

        return template.update(sql, params)
    }

    fun get(hendelseId: UUID): Journalforingstatus {
        val sql =
            """
            select * 
            from journalforingstatus
            where hendelse_id = :hendelse_id
            """.trimIndent()

        return template.queryForObject(
            sql,
            mapOf("hendelse_id" to hendelseId),
            journalforingstatusRowMapper,
        )
    }

    companion object {
        private val journalforingstatusRowMapper = RowMapper { rs, _ ->
            Journalforingstatus(
                hendelseId = UUID.fromString(rs.getString("hendelse_id")),
                journalpostId = rs.getString("journalpost_id"),
                bestillingsId = UUID.fromString(rs.getString("bestillingsid")),
                kanIkkeDistribueres = rs.getBoolean("kan_ikke_distribueres"),
                kanIkkeJournalfores = rs.getBoolean("kan_ikke_journalfores"),
            )
        }
    }
}
