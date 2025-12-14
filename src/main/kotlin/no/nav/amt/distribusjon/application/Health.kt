package no.nav.amt.distribusjon.application

import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.util.AttributeKey

val isReadyKey = AttributeKey<Boolean>("isReady")

fun Routing.registerHealthApi() {
    get("/internal/health/liveness") {
        call.respondText("I'm alive!")
    }

    get("/internal/health/readiness") {
        call.respondText("I'm ready!")
    }
}
