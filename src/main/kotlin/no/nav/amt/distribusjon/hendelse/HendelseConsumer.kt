package no.nav.amt.distribusjon.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.hendelse.db.HendelseDbo
import no.nav.amt.distribusjon.hendelse.db.HendelseRepository
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.kafka.Consumer
import no.nav.amt.distribusjon.kafka.ManagedKafkaConsumer
import no.nav.amt.distribusjon.kafka.config.KafkaConfig
import no.nav.amt.distribusjon.kafka.config.KafkaConfigImpl
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import no.nav.amt.distribusjon.varsel.VarselService
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.UUIDDeserializer
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class HendelseConsumer(
    private val varselService: VarselService,
    private val journalforingService: JournalforingService,
    private val hendelseRepository: HendelseRepository,
    groupId: String = Environment.KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.DELTAKER_HENDELSE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = UUIDDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String) {
        val hendelse: Hendelse = objectMapper.readValue(value)
        log.info("Mottatt hendelse ${hendelse.id} for deltaker ${hendelse.deltaker.id}")
        hendelseRepository.insert(
            HendelseDbo(
                id = hendelse.id,
                deltakerId = hendelse.deltaker.id,
                deltaker = hendelse.deltaker,
                ansvarlig = hendelse.ansvarlig,
                payload = hendelse.payload,
                opprettet = LocalDateTime.now(),
            ),
        )
        varselService.handleHendelse(hendelse)
        journalforingService.handleHendelse(hendelse)
    }

    override fun run() = consumer.run()
}
