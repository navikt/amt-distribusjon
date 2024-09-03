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
import no.nav.amt.distribusjon.application.plugins.configureAuthentication
import no.nav.amt.distribusjon.application.plugins.configureMonitoring
import no.nav.amt.distribusjon.application.plugins.configureRouting
import no.nav.amt.distribusjon.application.plugins.configureSerialization
import no.nav.amt.distribusjon.auth.AzureAdTokenClient
import no.nav.amt.distribusjon.digitalbruker.DigitalBrukerService
import no.nav.amt.distribusjon.distribusjonskanal.DokdistkanalClient
import no.nav.amt.distribusjon.hendelse.HendelseConsumer
import no.nav.amt.distribusjon.hendelse.HendelseRepository
import no.nav.amt.distribusjon.journalforing.JournalforingService
import no.nav.amt.distribusjon.journalforing.JournalforingstatusRepository
import no.nav.amt.distribusjon.journalforing.dokarkiv.DokarkivClient
import no.nav.amt.distribusjon.journalforing.dokdistfordeling.DokdistfordelingClient
import no.nav.amt.distribusjon.journalforing.job.EndringsvedtakJob
import no.nav.amt.distribusjon.journalforing.pdf.PdfgenClient
import no.nav.amt.distribusjon.journalforing.person.AmtPersonClient
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseProducer
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseRepository
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService
import no.nav.amt.distribusjon.utils.job.JobManager
import no.nav.amt.distribusjon.utils.job.leaderelection.LeaderElection
import no.nav.amt.distribusjon.varsel.VarselJobService
import no.nav.amt.distribusjon.varsel.VarselProducer
import no.nav.amt.distribusjon.varsel.VarselRepository
import no.nav.amt.distribusjon.varsel.VarselService
import no.nav.amt.distribusjon.varsel.hendelse.VarselHendelseConsumer
import no.nav.amt.distribusjon.veilarboppfolging.VeilarboppfolgingClient
import no.nav.amt.lib.utils.database.Database

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

    Database.init(environment.databaseConfig)

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
    val veilarboppfolgingClient = VeilarboppfolgingClient(httpClient, azureAdTokenClient, environment)
    val dokarkivClient = DokarkivClient(httpClient, azureAdTokenClient, environment)
    val dokdistkanalClient = DokdistkanalClient(httpClient, azureAdTokenClient, environment)
    val dokdistfordelingClient = DokdistfordelingClient(httpClient, azureAdTokenClient, environment)

    val digitalBrukerService = DigitalBrukerService(dokdistkanalClient, veilarboppfolgingClient)

    val unleash = DefaultUnleash(
        UnleashConfig
            .builder()
            .appName(Environment.appName)
            .instanceId(Environment.appName)
            .unleashAPI("${Environment.unleashUrl}/api")
            .apiKey(Environment.unleashToken)
            .build(),
    )

    val hendelseRepository = HendelseRepository()

    val varselService = VarselService(VarselRepository(), VarselProducer(), unleash)
    val journalforingService = JournalforingService(
        JournalforingstatusRepository(),
        amtPersonClient,
        pdfgenClient,
        veilarboppfolgingClient,
        dokarkivClient,
        dokdistfordelingClient,
    )

    val tiltakshendelseService = TiltakshendelseService(TiltakshendelseRepository(), TiltakshendelseProducer())

    val consumers = listOf(
        HendelseConsumer(
            varselService,
            journalforingService,
            tiltakshendelseService,
            hendelseRepository,
            dokdistkanalClient,
            veilarboppfolgingClient,
        ),
        VarselHendelseConsumer(varselService),
    )
    consumers.forEach { it.run() }

    configureAuthentication(environment)
    configureRouting(digitalBrukerService)
    configureMonitoring()

    val jobManager = JobManager(leaderElection, ::isReady)

    val endringsvedtakJob = EndringsvedtakJob(jobManager, hendelseRepository, journalforingService)
    endringsvedtakJob.startJob()

    val varselJobService = VarselJobService(jobManager, varselService)
    varselJobService.startJobs()

    attributes.put(isReadyKey, true)
}

fun Application.isReady() = attributes.getOrNull(isReadyKey) == true
