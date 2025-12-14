package no.nav.amt.distribusjon.utils

import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.utils.DbUtils.toPGObject
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class TestRepository(
    private val template: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val tablesToClean = setOf(
            "varsel",
            "journalforingstatus",
            "hendelse",
        )
    }

    fun cleanDatabase() = tablesToClean
        .map { "DELETE FROM $it" }
        .forEach { currentSql ->
            template.update(currentSql, emptyMap<String, Any>())
        }

    fun insert(hendelse: Hendelse) {
        val sql =
            """
            insert into hendelse (id, deltaker_id, deltaker, ansvarlig, payload, distribusjonskanal, manuelloppfolging, created_at)
            values(:id, :deltaker_id, :deltaker, :ansvarlig, :payload, :distribusjonskanal, :manuelloppfolging, :created_at)
            on conflict (id) do nothing
            """.trimIndent()

        val params = mapOf(
            "id" to hendelse.id,
            "deltaker_id" to hendelse.deltaker.id,
            "deltaker" to toPGObject(hendelse.deltaker, objectMapper),
            "ansvarlig" to toPGObject(hendelse.ansvarlig, objectMapper),
            "payload" to toPGObject(hendelse.payload, objectMapper),
            "created_at" to hendelse.opprettet,
            "distribusjonskanal" to hendelse.distribusjonskanal.name,
            "manuelloppfolging" to hendelse.manuellOppfolging,
        )

        template.update(sql, params)
    }
}
