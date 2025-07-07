package no.nav.amt.distribusjon.internal

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.amt.distribusjon.application.plugins.AuthorizationException
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService
import no.nav.amt.lib.outbox.OutboxService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

fun Routing.registerInternalApi(tiltakshendelseService: TiltakshendelseService, outboxService: OutboxService) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    post("/internal/forslag/ferdigstill/{forslagId}") {
        if (isInternal(call.request.local.remoteAddress)) {
            val forslagId = UUID.fromString(call.parameters["forslagId"])
            tiltakshendelseService.stoppForslagHendelse(forslagId)
            log.info("Ferdigstilt tiltakshendelse for forslag med id $forslagId")
            call.respond(HttpStatusCode.OK)
        } else {
            throw AuthorizationException("Ikke tilgang til api")
        }
    }

    post("/internal/tiltakshendelse/reproduser/{id}") {
        if (!isInternal(call.request.local.remoteAddress)) {
            throw AuthorizationException("Ikke tilgang til api")
        }
        val id = UUID.fromString(call.parameters["id"])
        tiltakshendelseService.reproduser(id)
        call.respond(HttpStatusCode.OK)
    }

    get("/internal/tiltakshendelse/fix") {
        if (!isInternal(call.request.local.remoteAddress)) {
            throw AuthorizationException("Ikke tilgang")
        }
        tiltakshendelseService.reproduserFeilproduserteHendelser()
    }
}

fun isInternal(remoteAdress: String): Boolean = remoteAdress == "127.0.0.1"
