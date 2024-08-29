package no.nav.amt.distribusjon.utils

import kotliquery.queryOf
import no.nav.amt.distribusjon.Environment
import no.nav.amt.distribusjon.db.Database
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.utility.DockerImageName

object SingletonPostgresContainer {
    private val log = LoggerFactory.getLogger(javaClass)

    private val postgresDockerImageName = getPostgresImage()

    private var postgresContainer: PostgreSQLContainer<Nothing>? = null

    fun start() {
        if (postgresContainer == null) {
            log.info("Starting new postgres database...")

            val container = createContainer()
            postgresContainer = container

            container.start()

            configureEnv(container)

            Database.init(Environment())

            setupShutdownHook()
            log.info("Postgres setup finished")
        }
    }

    private fun configureEnv(container: PostgreSQLContainer<Nothing>) {
        System.setProperty(Environment.DB_HOST_KEY, container.host)
        System.setProperty(Environment.DB_PORT_KEY, container.getMappedPort(POSTGRESQL_PORT).toString())
        System.setProperty(Environment.DB_DATABASE_KEY, container.databaseName)
        System.setProperty(Environment.DB_PASSWORD_KEY, container.password)
        System.setProperty(Environment.DB_USERNAME_KEY, container.username)
    }

    private fun createContainer(): PostgreSQLContainer<Nothing> {
        val container = PostgreSQLContainer<Nothing>(DockerImageName.parse(postgresDockerImageName).asCompatibleSubstituteFor("postgres"))
        container.addEnv("TZ", "Europe/Oslo")
        container.withReuse(Environment.testContainersReuse)
        container.withLabel("reuse.UUID", "dc04f4eb-01b6-4e32-b878-f0663d583a52")
        return container.waitingFor(HostPortWaitStrategy())
    }

    private fun setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Shutting down postgres database...")
                if (Environment.testContainersReuse) {
                    cleanup()
                } else {
                    postgresContainer?.stop()
                }
                Database.close()
            },
        )
    }

    fun cleanup() = Database.query {
        val tables = it.run(
            queryOf("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
                .map { it.string("table_name") }
                .asList,
        )

        it.transaction { tx ->
            tables.forEach { table ->
                val sql = "truncate table $table cascade"
                log.info("Truncating table $table...")
                tx.run(queryOf(sql).asExecute)
                val drop = "drop table $table"
                log.info("Dropping table $table...")
                tx.run(queryOf(drop).asExecute)
            }
        }
    }

    private fun getPostgresImage(): String = "postgres:16-alpine"
}
