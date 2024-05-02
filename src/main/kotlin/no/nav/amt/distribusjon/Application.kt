package no.nav.amt.distribusjon

import io.getunleash.DefaultUnleash
import io.getunleash.util.UnleashConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.amt.distribusjon.Environment.Companion.HTTP_CLIENT_TIMEOUT_MS
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.application.plugins.applicationConfig
import no.nav.amt.distribusjon.application.plugins.configureMonitoring
import no.nav.amt.distribusjon.application.plugins.configureRouting
import no.nav.amt.distribusjon.application.plugins.configureSerialization
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.db.Database
import no.nav.amt.distribusjon.hendelse.HendelseConsumer
import no.nav.amt.distribusjon.journalforing.EndringshendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.journalforing.JournalforingstatusRepository
import no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient
import no.nav.amt.distribusjon.journalforing.job.EndringshendelseJob
import no.nav.amt.distribusjon.journalforing.job.leaderelection.LeaderElection
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.journalforing.sak.SakClient
import no.nav.amt.distribusjon.varsel.VarselProducer
import no.nav.amt.distribusjon.varsel.VarselRepository
import no.nav.amt.distribusjon.varsel.VarselService
import no.nav.amt.distribusjon.varsel.hendelse.VarselHendelseConsumer

fun main() {
    val server = embeddedServer(Netty, port = 8080, module = Application::module)

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.application.attributes.put(isReadyKey, false)
            server.stop(gracePeriodMillis = 5_000, timeoutMillis = 30_000)
        },
    )
    server.start(wait = true)
}

fun Application.module() {
    configureSerialization()

    val environment = Environment()

    Database.init(environment)

    val httpClient = HttpClient(Apache) {
        engine {
            socketTimeout = HTTP_CLIENT_TIMEOUT_MS
            connectTimeout = HTTP_CLIENT_TIMEOUT_MS
            connectionRequestTimeout = HTTP_CLIENT_TIMEOUT_MS * 2
        }
        install(ContentNegotiation) {
            jackson { applicationConfig() }
        }
    }

    val leaderElection = LeaderElection(httpClient, environment.electorPath)

    val azureAdTokenClient = AzureAdTokenClient(httpClient, environment)
    val pdfgenClient = PdfgenClient(httpClient, environment)
    val amtPersonClient = AmtPersonClient(httpClient, azureAdTokenClient, environment)
    val sakClient = SakClient(httpClient, azureAdTokenClient, environment)
    val dokarkivClient = DokarkivClient(httpClient, azureAdTokenClient, environment)

    val unleash = DefaultUnleash(
        UnleashConfig.builder()
            .appName(Environment.appName)
            .instanceId(Environment.appName)
            .unleashAPI("${Environment.unleashUrl}/api")
            .apiKey(Environment.unleashToken)
            .build(),
    )

    val endringshendelseRepository = EndringshendelseRepository()

    val varselService = VarselService(VarselRepository(), VarselProducer(), unleash)
    val journalforingService = JournalforingService(
        JournalforingstatusRepository(),
        endringshendelseRepository,
        amtPersonClient,
        pdfgenClient,
        sakClient,
        dokarkivClient,
    )

    val consumers = listOf(
        HendelseConsumer(varselService, journalforingService),
        VarselHendelseConsumer(varselService),
    )
    consumers.forEach { it.run() }

    configureRouting()
    configureMonitoring()

    val endringshendelseJob = EndringshendelseJob(leaderElection, attributes, endringshendelseRepository, journalforingService)
    endringshendelseJob.startJob()

    attributes.put(isReadyKey, true)
}
