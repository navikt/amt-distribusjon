package no.nav.amt.distribusjon.varsel

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.kafka.Producer
import no.nav.amt.distribusjon.kafka.config.KafkaConfig
import no.nav.amt.distribusjon.kafka.config.KafkaConfigImpl
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import no.nav.amt.distribusjon.varsel.model.Varsel

class VarselProducer(
    kafkaConfig: KafkaConfig = if (Environment.isLocal()) LocalKafkaConfig() else KafkaConfigImpl(),
) {
    private val producer = Producer(kafkaConfig, Environment.MINSIDE_VARSEL_TOPIC)

    private val skalTogglesAv = !Environment.isLocal()

    fun inaktiver(varsel: Varsel) {
        if (skalTogglesAv) return

        producer.produce(
            key = varsel.deltakerId.toString(),
            value = varsel.toInaktiverDto(),
        )
    }

    fun opprettOppgave(varsel: Varsel) {
        if (skalTogglesAv) return

        require(varsel.type == Varsel.Type.PAMELDING) {
            "Kan ikke opprette oppgave, feil varseltype ${varsel.type}"
        }

        producer.produce(
            key = varsel.deltakerId.toString(),
            value = varsel.toOppgaveDto(true),
        )
    }

    fun opprettBeskjed(varsel: Varsel) {
        if (skalTogglesAv) return

        val skalVarslesEksternt = varsel.type == Varsel.Type.PAMELDING || varsel.type == Varsel.Type.AVSLUTTNING

        producer.produce(
            key = varsel.deltakerId.toString(),
            value = varsel.toBeskjedDto(skalVarslesEksternt),
        )
    }
}
