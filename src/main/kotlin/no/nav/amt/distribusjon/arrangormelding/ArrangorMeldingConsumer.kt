package no.nav.amt.distribusjon.arrangormelding

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService
import no.nav.amt.lib.kafka.Consumer
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.models.arrangor.melding.Forslag
import no.nav.amt.lib.models.arrangor.melding.Melding
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Component
class ArrangorMeldingConsumer(
    private val tiltakshendelseService: TiltakshendelseService,
    private val objectMapper: ObjectMapper,
    groupId: String = Environment.KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig,
) : Consumer<UUID, String?> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.ARRANGOR_MELDING_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String?) {
        val melding = value?.let { objectMapper.readValue<Melding>(value) }
        if (melding is Forslag) {
            log.info("Mottok forslag som skal distribueres på tiltakhendelse topic. deltakerId:${melding.deltakerId}")
            tiltakshendelseService.handleForslag(melding)
        }
    }

    override fun start() = consumer.start()

    override suspend fun close() = consumer.close()
}
