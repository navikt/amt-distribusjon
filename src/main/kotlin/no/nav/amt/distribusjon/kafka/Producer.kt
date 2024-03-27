package no.nav.amt.distribusjon.kafka

import no.nav.amt.distribusjon.kafka.config.KafkaConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

class Producer(private val kafkaConfig: KafkaConfig, private val topic: String) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun <K, V> produce(key: K, value: V) {
        val record = ProducerRecord(topic, key, value)

        KafkaProducer<K, V>(kafkaConfig.producerConfig()).use {
            val metadata = it.send(record).get()
            log.info(
                "Produserte melding til topic ${metadata.topic()}, " +
                    "key=${record.key()}, " +
                    "offset=${metadata.offset()}, " +
                    "partition=${metadata.partition()}",
            )
        }
    }
}
