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

    fun inaktiver(varsel: Varsel) {
        producer.produce(
            key = varsel.id.toString(),
            value = varsel.toInaktiverDto(),
        )
    }

    fun opprettOppgave(varsel: Varsel) {
        require(varsel.type == Varsel.Type.OPPGAVE) {
            "Kan ikke opprette oppgave, feil varseltype ${varsel.type}"
        }

        producer.produce(
            key = varsel.id.toString(),
            value = varsel.toOppgaveDto(true),
        )
    }

    fun opprettBeskjed(varsel: Varsel, skalVarsleEksternt: Boolean) {
        require(varsel.type == Varsel.Type.BESKJED) {
            "Kan ikke opprette beskjed, feil varseltype ${varsel.type}"
        }

        producer.produce(
            key = varsel.id.toString(),
            value = varsel.toBeskjedDto(skalVarsleEksternt),
        )
    }
}
