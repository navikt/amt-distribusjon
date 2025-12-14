package no.nav.amt.distribusjon

import no.nav.amt.distribusjon.IntegrationTestBase.Companion.kafkaContainer
import no.nav.amt.distribusjon.outbox.metrics.NoOpOutboxMeter
import no.nav.amt.distribusjon.outbox.metrics.OutboxMeter
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration(proxyBeanMethods = false)
class KafkaTestConfiguration {
    @Bean
    @Primary
    fun testKafkaConfig(): KafkaConfig = LocalKafkaConfig(kafkaBrokers = kafkaContainer.bootstrapServers)

    @Bean
    @Primary
    fun outboxMeter(): OutboxMeter = NoOpOutboxMeter()
}
