package no.nav.amt.distribusjon.varsel.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.kafka.Consumer
import no.nav.amt.distribusjon.kafka.ManagedKafkaConsumer
import no.nav.amt.distribusjon.kafka.config.KafkaConfig
import no.nav.amt.distribusjon.kafka.config.KafkaConfigImpl
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import no.nav.amt.distribusjon.varsel.VarselService
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.nowUTC
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

class VarselHendelseConsumer(
    private val varselService: VarselService,
    groupId: String = Environment.KAFKA_CONSUMER_GROUP_ID,
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<String, String> {
    private val log = LoggerFactory.getLogger(javaClass)

    private val consumer = ManagedKafkaConsumer(
        topic = Environment.MINSIDE_VARSEL_HENDELSE_TOPIC,
        config = kafkaConfig.consumerConfig(
            keyDeserializer = StringDeserializer(),
            valueDeserializer = StringDeserializer(),
            groupId = groupId,
        ),
        consume = ::consume,
    )

    override suspend fun consume(key: String, value: String) {
        val hendelse = objectMapper.readValue<VarselHendelseDto>(value)
        if (hendelse.namespace != Environment.namespace && hendelse.appnavn != Environment.appName) {
            return
        }

        val varselId = UUID.fromString(key)

        varselService.get(varselId).onSuccess {
            handterVarselHendelse(it, objectMapper.readValue(value))
        }
    }

    private fun handterVarselHendelse(varsel: Varsel, hendelse: VarselHendelseDto) {
        when (hendelse) {
            is EksternStatusHendelse -> {
                log.info("Ekstern varsling for varsel ${varsel.id} er ${hendelse.status}")
            }

            is InaktivertVarselHendelse -> {
                if (varsel.type == Varsel.Type.OPPGAVE || !varsel.erAktiv) return

                if (varsel.aktivTil != null && varsel.aktivTil <= nowUTC()) {
                    varselService.utlopBeskjed(varsel)
                }
            }
            is OpprettetVarselHendelse,
            is SlettetVarselHendelse,
            -> {
            }
        }
    }

    override fun run() = consumer.run()
}
