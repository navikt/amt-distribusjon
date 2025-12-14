package no.nav.amt.distribusjon

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

object TestUtils {
    val staticObjectMapper: ObjectMapper = JsonMapper
        .builder()
        .apply { addModule(KotlinModule.Builder().build()) }
        .build()
}
