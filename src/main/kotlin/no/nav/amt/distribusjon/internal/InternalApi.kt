package no.nav.amt.distribusjon.internal

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import no.nav.amt.distribusjon.application.plugins.AuthorizationException
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerInternalApi(tiltakshendelseService: TiltakshendelseService) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    post("/internal/forslag/ferdigstill/{forslagId}") {
        if (isInternal(call.request.local.remoteAddress)) {
            val forslagId = UUID.fromString(call.parameters["forslagId"])
            tiltakshendelseService.stoppForslagHendelse(forslagId)
            log.info("Ferdigstilt tiltakshendelse for forlag med id $forslagId")
            call.respond(HttpStatusCode.OK)
        } else {
            throw AuthorizationException("Ikke tilgang til api")
        }
    }
}

fun isInternal(remoteAdress: String): Boolean {
    return remoteAdress == "127.0.0.1"
}
