package no.nav.amt.distribusjon.utils

import kotlinx.coroutines.runBlocking
import no.nav.amt.distribusjon.Environment
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.slf4j.LoggerFactory
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

object SingletonKafkaProvider {
    private val log = LoggerFactory.getLogger(javaClass)
    private var kafkaContainer: KafkaContainer? = null

    val topics = listOf(
        Environment.DELTAKER_HENDELSE_TOPIC,
        Environment.MINSIDE_VARSEL_TOPIC,
        Environment.MINSIDE_VARSEL_HENDELSE_TOPIC,
    )

    fun start() {
        log.info("Starting new Kafka Instance...")
        kafkaContainer = KafkaContainer(DockerImageName.parse(getKafkaImage()))
        kafkaContainer!!.withReuse(Environment.testContainersReuse)
        kafkaContainer!!.withLabel("reuse.UUID", "37b4361b-5adc-4de0-823b-f42cc00d7206")
        kafkaContainer!!.start()

        setupShutdownHook()
        log.info("Kafka setup finished listening on ${kafkaContainer!!.bootstrapServers}.")
    }

    fun getHost(): String {
        if (kafkaContainer == null) {
            runBlocking { start() }
        }
        return kafkaContainer!!.bootstrapServers
    }

    private fun setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Shutting down Kafka server...")
                if (Environment.testContainersReuse) {
                    cleanup()
                } else {
                    kafkaContainer?.stop()
                }
            },
        )
    }

    fun cleanup() {
        val adminClient = AdminClient.create(
            mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaContainer!!.bootstrapServers),
        )
        topics.forEach {
            try {
                adminClient.deleteTopics(listOf(it))
                log.info("Deleted topic $it")
            } catch (e: Exception) {
                log.warn("Could not delete topic $it", e)
            }
        }
        adminClient.close()
    }

    private fun getKafkaImage(): String {
        val tag = when (System.getProperty("os.arch")) {
            "aarch64" -> "7.6.0-2-ubi8.arm64"
            else -> "7.6.0"
        }

        return "confluentinc/cp-kafka:$tag"
    }
}
