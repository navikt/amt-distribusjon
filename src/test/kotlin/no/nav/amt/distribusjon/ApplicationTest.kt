package no.nav.amt.distribusjon

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import junit.framework.TestCase.assertEquals
import no.nav.amt.distribusjon.application.plugins.configureRouting
import no.nav.amt.distribusjon.application.plugins.configureSerialization
import org.junit.Test

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureSerialization()
            configureRouting()
        }
        client.get("/internal/health/liveness").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("I'm alive!", bodyAsText())
        }
    }
}
