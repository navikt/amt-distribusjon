package no.nav.amt.distribusjon.varsel

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.lib.kafka.Producer

class VarselProducer(
    private val producer: Producer<String, String>,
) {
    fun inaktiver(varsel: Varsel) {
        producer.produce(
            topic = Environment.MINSIDE_VARSEL_TOPIC,
            key = varsel.id.toString(),
            value = varsel.toInaktiverDto(),
        )
    }

    fun opprettOppgave(varsel: Varsel) {
        require(varsel.type == Varsel.Type.OPPGAVE) {
            "Kan ikke opprette oppgave, feil varseltype ${varsel.type}"
        }

        producer.produce(
            topic = Environment.MINSIDE_VARSEL_TOPIC,
            key = varsel.id.toString(),
            value = varsel.toOppgaveDto(),
        )
    }

    fun opprettBeskjed(varsel: Varsel) {
        require(varsel.type == Varsel.Type.BESKJED) {
            "Kan ikke opprette beskjed, feil varseltype ${varsel.type}"
        }

        producer.produce(
            topic = Environment.MINSIDE_VARSEL_TOPIC,
            key = varsel.id.toString(),
            value = varsel.toBeskjedDto(),
        )
    }
}
