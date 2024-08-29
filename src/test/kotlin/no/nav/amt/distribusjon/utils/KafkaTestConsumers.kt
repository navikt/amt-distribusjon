package no.nav.amt.distribusjon.utils

import kotlinx.coroutines.runBlocking
import no.nav.amt.lib.kafka.ManagedKafkaConsumer
import no.nav.amt.lib.kafka.config.LocalKafkaConfig
import no.nav.amt.lib.testing.SingletonKafkaProvider
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.UUID

fun stringStringConsumer(topic: String, block: suspend (k: String, v: String) -> Unit): ManagedKafkaConsumer<String, String> {
    val config = LocalKafkaConfig(SingletonKafkaProvider.getHost(), "earliest").consumerConfig(
        keyDeserializer = StringDeserializer(),
        valueDeserializer = StringDeserializer(),
        groupId = "test-consumer-${UUID.randomUUID()}",
    )

    return ManagedKafkaConsumer(topic, config, block)
}

fun assertProduced(topic: String, block: suspend (cache: Map<UUID, String>) -> Unit) {
    val cache = mutableMapOf<UUID, String>()

    val consumer = stringStringConsumer(topic) { k, v ->
        cache[UUID.fromString(k)] = v
    }

    runBlocking {
        consumer.run()
        block(cache)
        consumer.stop()
    }
}
