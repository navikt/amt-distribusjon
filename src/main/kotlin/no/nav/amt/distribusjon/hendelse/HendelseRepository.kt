package no.nav.amt.distribusjon.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Row
import kotliquery.queryOf
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.db.toPGObject
import no.nav.amt.distribusjon.distribusjonskanal.Distribusjonskanal
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.journalforing.model.HendelseMedJournalforingstatus
import no.nav.amt.distribusjon.journalforing.model.Journalforingstatus
import java.time.LocalDateTime

class HendelseRepository {
    private fun rowmapper(row: Row) = Hendelse(
        id = row.uuid("id"),
        deltaker = objectMapper.readValue(row.string("deltaker")),
        ansvarlig = objectMapper.readValue(row.string("ansvarlig")),
        payload = objectMapper.readValue(row.string("payload")),
        opprettet = row.localDateTime("h.created_at"),
        distribusjonskanal = row.string("distribusjonskanal").let { Distribusjonskanal.valueOf(it) },
        manuellOppfolging = row.boolean("manuelloppfolging"),
    )

    private fun rowmapperHendelseMedJournalforingstatus(row: Row) = HendelseMedJournalforingstatus(
        hendelse = Hendelse(
            id = row.uuid("id"),
            deltaker = objectMapper.readValue(row.string("deltaker")),
            ansvarlig = objectMapper.readValue(row.string("ansvarlig")),
            payload = objectMapper.readValue(row.string("payload")),
            opprettet = row.localDateTime("h.created_at"),
            distribusjonskanal = row.string("distribusjonskanal").let { Distribusjonskanal.valueOf(it) },
            manuellOppfolging = row.boolean("manuelloppfolging"),
        ),
        journalforingstatus = Journalforingstatus(
            hendelseId = row.uuid("id"),
            journalpostId = row.stringOrNull("journalpost_id"),
            bestillingsId = row.uuidOrNull("bestillingsid"),
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

    fun getIkkeJournalforteHendelser(opprettet: LocalDateTime) = Database.query {
        val sql =
            """
            select h.id as "id",
                h.deltaker as "deltaker",
                h.ansvarlig as "ansvarlig",
                h.payload as "payload",
                h.created_at as "h.created_at",
                h.distribusjonskanal as "distribusjonskanal",
                h.manuelloppfolging as "manuelloppfolging",
                js.journalpost_id as "journalpost_id",
                js.bestillingsid as "bestillingsid"
            from hendelse h
                left join journalforingstatus js on h.id = js.hendelse_id
            where h.created_at < :opprettet 
                and js.hendelse_id is not null
                and (js.journalpost_id is null or (js.bestillingsid is null and h.distribusjonskanal not in ('DITT_NAV','SDP')))
            """.trimIndent()

        val query = queryOf(sql, mapOf("opprettet" to opprettet))

        it.run(query.map(::rowmapperHendelseMedJournalforingstatus).asList)
    }
}
