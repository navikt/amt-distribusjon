package no.nav.amt.distribusjon.job

import no.nav.amt.distribusjon.exchangeWithLogging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.InetAddress

@Component
class LeaderElection(
    clientBuilder: RestClient.Builder,
    @Value($$"${elector.path}") private val electorPath: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun isLeader(): Boolean = if (electorPath == DONT_LOOK_FOR_LEADER) {
        log.info("Ser ikke etter leader, returnerer at jeg er leader")
        true
    } else {
        val leader: Leader = httpClient.get().exchangeWithLogging("Kall mot elector feilet")
        leader.name == InetAddress.getLocalHost().hostName
    }

    val uriString =
        UriComponentsBuilder
            .fromUriString(
                if (electorPath.startsWith("http://")) {
                    electorPath
                } else {
                    "http://$electorPath"
                },
            ).toUriString()

    private val httpClient = clientBuilder.baseUrl(uriString).build()

    companion object {
        const val DONT_LOOK_FOR_LEADER = "dont_look_for_leader"
    }

    private data class Leader(
        val name: String,
    )
}
