package no.nav.amt.distribusjon.tiltakshendelse

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.distribusjon.tiltakshendelse.model.toDto
import no.nav.amt.lib.kafka.Producer
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class TiltakshendelseProducer(
    private val producer: Producer<String, String>,
    private val objectMapper: ObjectMapper,
) {
    fun produce(tiltakshendelse: Tiltakshendelse) = producer.produce(
        topic = Environment.TILTAKSHENDELSE_TOPIC,
        key = tiltakshendelse.id.toString(),
        value = objectMapper.writeValueAsString(tiltakshendelse.toDto()),
    )
}
