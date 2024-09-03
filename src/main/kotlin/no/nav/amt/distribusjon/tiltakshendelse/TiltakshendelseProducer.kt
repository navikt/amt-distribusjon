package no.nav.amt.distribusjon.tiltakshendelse

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.tiltakshendelse.model.Tiltakshendelse
import no.nav.amt.distribusjon.tiltakshendelse.model.toDto
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig

class TiltakshendelseProducer(
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {
    private val producer = Producer(kafkaConfig, Environment.TILTAKSHENDELSE_TOPIC)

    fun produce(tiltakshendelse: Tiltakshendelse) {
        producer.produce(
            key = tiltakshendelse.id.toString(),
            value = objectMapper.writeValueAsString(tiltakshendelse.toDto()),
        )
    }
}
