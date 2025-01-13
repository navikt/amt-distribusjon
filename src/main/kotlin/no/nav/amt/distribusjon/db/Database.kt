package no.nav.amt.distribusjon.db

import no.nav.amt.distribusjon.application.plugins.objectMapper
import org.postgresql.util.PGobject

fun toPGObject(value: Any?) = PGobject().also {
    it.type = "json"
    it.value = value?.let { v -> objectMapper.writeValueAsString(v) }
}
