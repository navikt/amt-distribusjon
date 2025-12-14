package no.nav.amt.distribusjon.utils

import org.postgresql.util.PGobject
import tools.jackson.databind.ObjectMapper

// fun <V> sqlParameters(vararg pairs: Pair<String, V>): MapSqlParameterSource = MapSqlParameterSource().addValues(pairs.toMap())

object DbUtils {
    fun toPGObject(value: Any?, objectMapper: ObjectMapper) = PGobject().also {
        it.type = "json"
        it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
    }
}
