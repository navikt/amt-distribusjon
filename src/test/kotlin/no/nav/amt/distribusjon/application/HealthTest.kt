package no.nav.amt.distribusjon.application

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import no.nav.amt.distribusjon.RouteTestBase
import org.junit.Test

class HealthTest : RouteTestBase() {
    @Test
    fun `liveness-endepunktet skal returnere 200 OK naar app er i live`() {
        val response = runInTestContext { httpClient ->
            httpClient.get("/internal/health/liveness")
        }

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `readiness-endepunktet skal returnere 503 ServiceUnavailable naar app ikke er klar`() {
        val response = runInTestContext(false) { httpClient ->
            httpClient.get("/internal/health/readiness")
        }

        response.status shouldBe HttpStatusCode.ServiceUnavailable
    }

    @Test
    fun `readiness-endepunktet skal returnere 200 OK naar app er klar`() {
        val response = runInTestContext { httpClient ->
            httpClient.get("/internal/health/readiness")
        }

        response.status shouldBe HttpStatusCode.OK
    }
}
