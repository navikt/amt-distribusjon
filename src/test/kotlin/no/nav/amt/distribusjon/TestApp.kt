package no.nav.amt.distribusjon

import io.getunleash.FakeUnleash
import io.ktor.client.HttpClient
import io.ktor.server.testing.testApplication
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.application.plugins.configureRouting
import no.nav.amt.distribusjon.application.plugins.configureSerialization
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.hendelse.HendelseConsumer
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.journalforing.sak.SakClient
import no.nav.amt.distribusjon.kafka.config.LocalKafkaConfig
import no.nav.amt.distribusjon.utils.SingletonKafkaProvider
import no.nav.amt.distribusjon.utils.SingletonPostgresContainer
import no.nav.amt.distribusjon.utils.mockAmtPersonClient
import no.nav.amt.distribusjon.utils.mockAzureAdClient
import no.nav.amt.distribusjon.utils.mockPdfgenClient
import no.nav.amt.distribusjon.utils.mockSakClient
import no.nav.amt.distribusjon.varsel.VarselProducer
import no.nav.amt.distribusjon.varsel.VarselRepository
import no.nav.amt.distribusjon.varsel.VarselService
import no.nav.amt.distribusjon.varsel.hendelse.VarselHendelseConsumer
import java.util.UUID

class TestApp {
    val varselRepository: VarselRepository
    val varselService: VarselService

    val azureAdTokenClient: AzureAdTokenClient

    val pdfgenClient: PdfgenClient
    val amtPersonClient: AmtPersonClient
    val sakClient: SakClient
    val journalforingService: JournalforingService

    val unleash: FakeUnleash

    init {
        SingletonPostgresContainer.start()
        SingletonKafkaProvider.start()

        val environment = Environment()

        unleash = FakeUnleash()
        unleash.enableAll()

        azureAdTokenClient = mockAzureAdClient(environment)
        pdfgenClient = mockPdfgenClient(environment)
        amtPersonClient = mockAmtPersonClient(azureAdTokenClient, environment)
        sakClient = mockSakClient(azureAdTokenClient, environment)

        varselRepository = VarselRepository()
        varselService = VarselService(varselRepository, VarselProducer(LocalKafkaConfig(SingletonKafkaProvider.getHost())), unleash)

        journalforingService = JournalforingService(amtPersonClient, pdfgenClient, sakClient)

        val consumerId = UUID.randomUUID().toString()
        val kafkaConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost())
        val consumers = listOf(
            HendelseConsumer(varselService, journalforingService, consumerId, kafkaConfig),
            VarselHendelseConsumer(varselService, consumerId, kafkaConfig),
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
