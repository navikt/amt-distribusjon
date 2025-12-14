package no.nav.amt.distribusjon.config

import no.nav.amt.distribusjon.job.LeaderElection
import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.outbox.OutboxProcessor
import no.nav.amt.lib.outbox.OutboxService
import no.nav.amt.lib.utils.job.JobManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration(proxyBeanMethods = false)
class OutboxConfig {
    @Bean
    fun outboxService(): OutboxService = OutboxService()

    @Bean
    fun jobManager(leaderElection: LeaderElection): JobManager = JobManager(
        isLeader = leaderElection::isLeader,
        applicationIsReady = { true }, // TODO
    )

    @Bean
    fun outboxProcessor(
        outboxService: OutboxService,
        jobManager: JobManager,
        kafkaProducer: Producer<String, String>,
    ): OutboxProcessor = OutboxProcessor(
        service = outboxService,
        jobManager = jobManager,
        producer = kafkaProducer,
    )

    @Bean
    @Profile("!test")
    fun kafkaProducer() = Producer<String, String>(KafkaConfigImpl())

    @Bean
    @Profile("test")
    fun testKafkaProducer() = Producer<String, String>(LocalKafkaConfig())
}
