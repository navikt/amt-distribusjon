package no.nav.amt.distribusjon.hendelse

import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.journalforing.model.HendelseMedJournalforingstatus
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import no.nav.amt.distribusjon.utils.DbUtils.toPGObject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID

@Service
class HendelseRepository(
    private val template: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    fun insert(hendelse: Hendelse): Int {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload, distribusjonskanal, manuelloppfolging)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload, :distribusjonskanal, :manuelloppfolging)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelse.id,
            "deltaker_id" to hendelse.deltaker.id,
            "deltaker" to toPGObject(hendelse.deltaker, objectMapper),
            "ansvarlig" to toPGObject(hendelse.ansvarlig, objectMapper),
            "payload" to toPGObject(hendelse.payload, objectMapper),
            "distribusjonskanal" to hendelse.distribusjonskanal.name,
            "manuelloppfolging" to hendelse.manuellOppfolging,
        )

        return template.update(sql, params)
    }

    fun getIkkeJournalforteHendelser(opprettet: LocalDateTime): List<HendelseMedJournalforingstatus> {
        val sql =
            """
            select h.id as "id",
                h.deltaker as "deltaker",
                h.ansvarlig as "ansvarlig",
                h.payload as "payload",
                h.created_at as "hendelse_created_at",
                h.distribusjonskanal as "distribusjonskanal",
                h.manuelloppfolging as "manuelloppfolging",
                js.journalpost_id as "journalpost_id",
                js.bestillingsid as "bestillingsid",
                js.kan_ikke_distribueres as "kan_ikke_distribueres",
                js.kan_ikke_journalfores as "kan_ikke_journalfores"
            from 
                hendelse h
                left join journalforingstatus js on h.id = js.hendelse_id
            where 
                h.created_at < :opprettet 
                and js.hendelse_id is not null
                and (js.journalpost_id is null 
                and 
                    (js.kan_ikke_journalfores is null or js.kan_ikke_journalfores = false)
                    or (
                        js.bestillingsid is null and h.distribusjonskanal not in ('DITT_NAV','SDP') 
                        and (js.kan_ikke_distribueres is null or js.kan_ikke_distribueres = false)
                    )
                )
            """.trimIndent()

        return template.query(
            sql,
            mapOf("opprettet" to opprettet),
            hendelseMedJournalforingstatusRowMapper,
        )
    }

    fun getHendelser(hendelseIder: List<UUID>): List<Hendelse> = if (hendelseIder.isEmpty()) {
        emptyList()
    } else {
        template.query(
            "SELECT * FROM hendelse WHERE id IN (:ids)",
            mapOf("ids" to hendelseIder),
            hendelseRowMapper,
        )
    }

    private val hendelseRowMapper = RowMapper { rs, _ ->
        Hendelse(
            id = UUID.fromString(rs.getString("id")),
            deltaker = objectMapper.readValue(rs.getString("deltaker")),
            ansvarlig = objectMapper.readValue(rs.getString("ansvarlig")),
            payload = objectMapper.readValue(rs.getString("payload")),
            opprettet = rs.getTimestamp("hendelse_created_at").toLocalDateTime(),
            distribusjonskanal = rs.getString("distribusjonskanal").let { Distribusjonskanal.valueOf(it) },
            manuellOppfolging = rs.getBoolean("manuelloppfolging"),
        )
    }

    private val journalforingstatusRowMapper = RowMapper { rs, _ ->
        Journalforingstatus(
            hendelseId = UUID.fromString(rs.getString("id")),
            journalpostId = rs.getString("journalpost_id"),
            bestillingsId = rs.getString("bestillingsid")?.let { UUID.fromString(it) },
            kanIkkeDistribueres = rs.getBoolean("kan_ikke_distribueres"),
            kanIkkeJournalfores = rs.getBoolean("kan_ikke_journalfores"),
        )
    }

    val hendelseMedJournalforingstatusRowMapper = RowMapper { rs, rowNum ->
        HendelseMedJournalforingstatus(
            hendelse = hendelseRowMapper.mapRow(rs, rowNum),
            journalforingstatus = journalforingstatusRowMapper.mapRow(rs, rowNum),
        )
    }
}
