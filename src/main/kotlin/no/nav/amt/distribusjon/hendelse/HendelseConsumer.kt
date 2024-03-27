package no.nav.amt.distribusjon.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.kafka.Consumer
import no.nav.amt.distribusjon.kafka.ManagedKafkaConsumer
import no.nav.amt.distribusjon.kafka.config.KafkaConfig
import no.nav.amt.distribusjon.kafka.config.KafkaConfigImpl
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import no.nav.amt.distribusjon.varsel.VarselService
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import java.util.UUID

class HendelseConsumer(
    private val varselService: VarselService,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String> {
    private val consumer = ManagedKafkaConsumer(
        topic = Environment.DELTAKER_HENDELSE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = Environment.KAFKA_CONSUMER_GROUP_ID,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String) {
        val hendelse: Hendelse = objectMapper.readValue(value)
        varselService.handleHendelse(hendelse)
    }

    override fun run() = consumer.run()
}
