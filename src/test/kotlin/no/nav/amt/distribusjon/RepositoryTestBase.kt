package no.nav.amt.distribusjon

import no.nav.amt.distribusjon.utils.TestRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureJdbc
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@ActiveProfiles("test")
@EnableMockOAuth2Server
@Import(TestRepository::class)
@AutoConfigureJdbc
@AutoConfigureJson
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class RepositoryTestBase {
    @Autowired
    protected lateinit var testRepository: TestRepository

    companion object {
        private const val POSTGRES_DOCKER_IMAGE_NAME = "postgres:17-alpine"

        @ServiceConnection
        @Suppress("unused")
        private val postgres = PostgreSQLContainer(
            DockerImageName
                .parse(POSTGRES_DOCKER_IMAGE_NAME)
                .asCompatibleSubstituteFor("postgres"),
        ).apply {
            addEnv("TZ", "Europe/Oslo")
            waitingFor(Wait.forListeningPort())
            withCommand("postgres", "-c", "wal_level=logical")
        }
    }
}
