package no.nav.amt.distribusjon

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ApplicationTest {
    @Test
    fun testRoot() = integrationTest { _, client ->
        client.get("/internal/health/liveness").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("I'm alive!", bodyAsText())
        }
    }
}
