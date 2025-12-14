package no.nav.amt.distribusjon

import io.ktor.client.HttpClient
import io.ktor.server.testing.testApplication
import no.nav.amt.distribusjon.TestUtils.staticObjectMapper
import no.nav.amt.distribusjon.application.plugins.configureAuthentication
import no.nav.amt.distribusjon.outbox.OutboxRecord
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseProducer
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.testing.SingletonKafkaProvider
import no.nav.amt.lib.testing.SingletonPostgres16Container
import java.util.UUID

class TestApp {
    val tiltakshendelseProducer: TiltakshendelseProducer

    // val outboxService: OutboxService

    val environment: Environment = testEnvironment

    init {
        @Suppress("UnusedExpression")
        SingletonPostgres16Container
        SingletonKafkaProvider.start()
        val kafakConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost())
        val kafkaProducer = Producer<String, String>(kafakConfig)

        // outboxService = OutboxService()

        tiltakshendelseProducer = TiltakshendelseProducer(kafkaProducer, staticObjectMapper)

        val consumerId = UUID.randomUUID().toString()
        val kafkaConfig = LocalKafkaConfig(SingletonKafkaProvider.getHost())
        /*
                val consumers = listOf(
                    HendelseConsumer(
                        varselService,
                        journalforingService,
                        tiltakshendelseService,
                        hendelseRepository,
                        dokdistkanalClient,
                        veilarboppfolgingClient,
                        consumerId,
                        kafkaConfig,
                    ),
                    VarselHendelseConsumer(varselService, consumerId, kafkaConfig),
                    ArrangorMeldingConsumer(tiltakshendelseService),
                )

                consumers.forEach { it.start() }
         */
    }
}

private val testApp = TestApp()

fun integrationTest(appShouldBeReady: Boolean = true, testBlock: suspend (app: TestApp, client: HttpClient) -> Unit) = testApplication {
    application {
        configureAuthentication(testApp.environment)
    }

    testBlock(
        testApp,
        createClient {
        },
    )
}

fun haveOutboxRecord(
    key: Any,
    topic: String,
    additionalConditions: (record: OutboxRecord) -> Boolean = { true },
) = object {
    fun test(value: TestApp) {
/*
        val records = value.outboxService.getRecordsByTopicAndKey(topic, key.toString())
        val passed = records.any(additionalConditions)
        return MatcherResult(
            passed,
            {
                if (records.isEmpty()) {
                    "Expected an outbox record with key `$key` for topic `$topic`, but found none."
                } else {
                    "Outbox record with key `$key` for topic `$topic` did not meet additional conditions."
                }
            },
            { "Expected no outbox record with key `$key` for topic `$topic` that meets the conditions, but found one." },
        )
*/
    }
}
