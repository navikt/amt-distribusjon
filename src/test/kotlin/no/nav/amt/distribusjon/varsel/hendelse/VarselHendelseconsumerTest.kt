package no.nav.amt.distribusjon.varsel.hendelse

import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.application.plugins.objectMapper
import no.nav.amt.distribusjon.integrationTest
import no.nav.amt.distribusjon.utils.AsyncUtils
import no.nav.amt.distribusjon.utils.data.Varselsdata
import no.nav.amt.distribusjon.utils.produceStringString
import no.nav.amt.distribusjon.utils.shouldBeCloseTo
import no.nav.amt.distribusjon.varsel.model.Varsel
import no.nav.amt.distribusjon.varsel.nowUTC
import no.nav.tms.varsel.action.Varseltype
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.Test

class VarselHendelseconsumerTest {
    @Test
    fun `inaktiver - aktiv beskjed finnes - inaktiverer`() = integrationTest { app, _ ->
        val beskjed = Varselsdata.varsel(type = Varsel.Type.BESKJED)
        app.varselRepository.upsert(beskjed)

        val hendelseDto = InaktivertVarselHendelse(
            beskjed.id.toString(),
            Varseltype.Beskjed,
            Environment.namespace,
            Environment.appName,
        )
        produce(hendelseDto)

        AsyncUtils.eventually {
            app.varselRepository.get(beskjed.id).getOrThrow().aktivTil!! shouldBeCloseTo nowUTC()
        }
    }
}

private fun produce(varselHendelseDto: VarselHendelseDto) = produceStringString(
    ProducerRecord(
        Environment.MINSIDE_VARSEL_HENDELSE_TOPIC,
        varselHendelseDto.varselId,
        objectMapper.writeValueAsString(varselHendelseDto),
    ),
)
