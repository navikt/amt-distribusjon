package no.nav.amt.distribusjon.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.journalforing.model.HendelseMedJournalforingstatus
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.DbUtils.toPGObject
import no.nav.amt.lib.utils.database.Database
import java.time.LocalDateTime
import java.util.UUID

class HendelseRepository {
    private fun hendelseRowMapper(row: Row) = Hendelse(
        id = row.uuid("id"),
        deltaker = objectMapper.readValue(row.string("deltaker")),
        ansvarlig = objectMapper.readValue(row.string("ansvarlig")),
        payload = objectMapper.readValue(row.string("payload")),
        opprettet = row.localDateTime("created_at"),
        distribusjonskanal = row.string("distribusjonskanal").let { Distribusjonskanal.valueOf(it) },
        manuellOppfolging = row.boolean("manuelloppfolging"),
    )

    private fun hendelseMedJournalforingstatusRowMapper(row: Row) = HendelseMedJournalforingstatus(
        hendelse = hendelseRowMapper(row),
        journalforingstatus = Journalforingstatus(
            hendelseId = row.uuid("id"),
            journalpostId = row.stringOrNull("journalpost_id"),
            bestillingsId = row.uuidOrNull("bestillingsid"),
            kanIkkeDistribueres = row.boolean("kan_ikke_distribueres"),
            kanIkkeJournalfores = row.boolean("kan_ikke_journalfores"),
        ),
    )

    fun insert(hendelse: Hendelse) = Database.query {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload, distribusjonskanal, manuelloppfolging)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload, :distribusjonskanal, :manuelloppfolging)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelse.id,
            "deltaker_id" to hendelse.deltaker.id,
            "deltaker" to toPGObject(hendelse.deltaker),
            "ansvarlig" to toPGObject(hendelse.ansvarlig),
            "payload" to toPGObject(hendelse.payload),
            "distribusjonskanal" to hendelse.distribusjonskanal.name,
            "manuelloppfolging" to hendelse.manuellOppfolging,
        )

        it.update(queryOf(sql, params))
    }

    private val ikkeJournalforteHendelserBaseSql =
        """
        SELECT
            h.id,
            h.deltaker,
            h.ansvarlig,
            h.payload,
            h.created_at,
            h.distribusjonskanal,
            h.manuelloppfolging,
            js.journalpost_id,
            js.bestillingsid,
            js.kan_ikke_distribueres,
            js.kan_ikke_journalfores
        FROM 
            hendelse h
            JOIN journalforingstatus js ON h.id = js.hendelse_id            
        """.trimIndent()

    fun getIkkeJournalforteHendelser(opprettet: LocalDateTime) = Database.query {
        val sql =
            """
            $ikkeJournalforteHendelserBaseSql                
            WHERE
                js.journalpost_id IS NULL
                AND js.kan_ikke_journalfores IS NOT TRUE
                AND h.created_at < :opprettet
                            
            UNION ALL
            
            $ikkeJournalforteHendelserBaseSql                
            WHERE
                js.bestillingsid IS NULL
                AND js.kan_ikke_distribueres IS NOT TRUE
                AND js.journalpost_id IS NOT NULL -- utelukker records fra første spørring
                AND h.distribusjonskanal NOT IN ('DITT_NAV','SDP')
                AND h.created_at < :opprettet                
            """.trimIndent()

        val query = queryOf(sql, mapOf("opprettet" to opprettet))

        it.run(query.map(::hendelseMedJournalforingstatusRowMapper).asList)
    }

    fun getHendelser(hendelseIder: List<UUID>) = Database.query {
        if (hendelseIder.isEmpty()) {
            return@query emptyList()
        }
        val sql =
            """
            select * from hendelse
            where id in (${hendelseIder.joinToString { "?" }})
            """.trimIndent()

        val query = queryOf(
            sql,
            *hendelseIder.toTypedArray(),
        ).map(::hendelseRowMapper).asList

        it.run(query)
    }
}
