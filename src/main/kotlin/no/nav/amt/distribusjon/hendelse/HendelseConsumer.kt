package no.nav.amt.distribusjon.hendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.hendelse.model.Hendelse
import no.nav.amt.distribusjon.hendelse.model.HendelseType
import no.nav.amt.distribusjon.kafka.Consumer
import no.nav.amt.distribusjon.kafka.ManagedKafkaConsumer
import no.nav.amt.distribusjon.kafka.config.KafkaConfig
import no.nav.amt.distribusjon.kafka.config.KafkaConfigImpl
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import java.util.UUID

class HendelseConsumer(
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) : Consumer<UUID, String> {
    private val consumer = ManagedKafkaConsumer(
        topic = Environment.DELTAKER_HENDELSE_TOPIC,
        config = kafkaConfig.commonConfig(),
        consume = ::consume,
    )

    override suspend fun consume(key: UUID, value: String) {
        val hendelse: Hendelse = objectMapper.readValue(value)

        when (hendelse.payload) {
            is HendelseType.AvbrytUtkast -> TODO()
            is HendelseType.AvsluttDeltakelse -> TODO()
            is HendelseType.EndreBakgrunnsinformasjon -> TODO()
            is HendelseType.EndreDeltakelsesmengde -> TODO()
            is HendelseType.EndreInnhold -> TODO()
            is HendelseType.EndreSluttarsak -> TODO()
            is HendelseType.EndreSluttdato -> TODO()
            is HendelseType.EndreStartdato -> TODO()
            is HendelseType.ForlengDeltakelse -> TODO()
            is HendelseType.IkkeAktuell -> TODO()
            is HendelseType.InnbyggerGodkjennUtkast -> TODO()
            is HendelseType.NavGodkjennUtkast -> TODO()
            is HendelseType.OpprettUtkast -> TODO()
        }
    }

    override fun run() = consumer.run()
}
