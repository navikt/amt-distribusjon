package no.nav.amt.distribusjon

import io.getunleash.FakeUnleash
import io.ktor.client.HttpClient
import io.ktor.server.testing.testApplication
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.application.plugins.configureRouting
import no.nav.amt.distribusjon.application.plugins.configureSerialization
import no.nav.amt.distribusjon.hendelse.HendelseConsumer
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import no.nav.amt.distribusjon.utils.SingletonKafkaProvider
import no.nav.amt.distribusjon.utils.SingletonPostgresContainer
import no.nav.amt.distribusjon.varsel.VarselProducer
import no.nav.amt.distribusjon.varsel.VarselRepository
import no.nav.amt.distribusjon.varsel.VarselService
import java.util.UUID

class TestApp {
    val varselRepository: VarselRepository
    val varselService: VarselService
    val unleash: FakeUnleash

    init {
        SingletonPostgresContainer.start()
        SingletonKafkaProvider.start()

        unleash = FakeUnleash()
        unleash.enableAll()

        varselRepository = VarselRepository()
        varselService = VarselService(varselRepository, VarselProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost())), unleash)

        val consumers = listOf(
            HendelseConsumer(varselService, UUID.randomUUID().toString(), kafkaConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost())),
        )

        consumers.forEach { it.run() }
    }
}

private val testApp = TestApp()

fun integrationTest(testBlock: suspend (app: TestApp, client: HttpClient) -> Unit) = testApplication {
    application {
        configureSerialization()

        configureRouting()
        // configureMonitoring()

        attributes.put(isReadyKey, true)
    }

    testBlock(testApp, client)
}
