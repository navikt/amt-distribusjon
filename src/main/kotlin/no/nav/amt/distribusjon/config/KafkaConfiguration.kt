package no.nav.amt.distribusjon.config

import no.nav.amt.lib.kafka.Producer
import no.nav.amt.lib.kafka.config.KafkaConfig
import no.nav.amt.lib.kafka.config.KafkaConfigImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class KafkaConfiguration {
    @Bean
    fun kafkaConfig(): KafkaConfig = KafkaConfigImpl()

    @Bean
    fun kafkaProducer(kafkaConfig: KafkaConfig) = Producer<String, String>(kafkaConfig)
}
