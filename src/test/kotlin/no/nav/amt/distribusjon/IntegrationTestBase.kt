package no.nav.amt.distribusjon

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(KafkaTestConfiguration::class)
abstract class IntegrationTestBase : RepositoryTestBase() {
    companion object {
        val kafkaContainer = KafkaContainer(DockerImageName.parse("apache/kafka")).apply {
            // workaround for https://github.com/testcontainers/testcontainers-java/issues/9506
            withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,BROKER://:9093,CONTROLLER://:9094")
            start()
            System.setProperty("KAFKA_BROKERS", bootstrapServers)
        }
    }
}
