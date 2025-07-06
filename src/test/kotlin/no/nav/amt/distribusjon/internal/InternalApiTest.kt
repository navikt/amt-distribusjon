package no.nav.amt.distribusjon.internal

import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import no.nav.amt.distribusjon.RouteTestBase
import org.junit.Test
import java.util.UUID

class InternalApiTest : RouteTestBase() {
    init {
        mockkStatic(::isInternal)
    }

    @Test
    fun `skal returnere 403 Forbidden naar ferdigstill-endepunkt treffes fra ekstern IP`() {
        every { isInternal(any()) } returns false

        val response = runInTestContext { httpClient ->
            httpClient.post("/internal/forslag/ferdigstill/${UUID.randomUUID()}")
        }
        response.status shouldBe HttpStatusCode.Forbidden

        coVerify { mockTiltakshendelseService wasNot Called }
    }

    @Test
    fun `skal returnere 200 OK og stoppe forslagshendelse naar ferdigstill-endepunkt treffes fra intern IP`() {
        every { isInternal(any()) } returns true

        val forslagId = UUID.randomUUID()

        val response = runInTestContext { httpClient ->
            httpClient.post("/internal/forslag/ferdigstill/$forslagId")
        }
        response.status shouldBe HttpStatusCode.OK

        coVerify { mockTiltakshendelseService.stoppForslagHendelse(forslagId) }
    }

    @Test
    fun `skal returnere 403 Forbidden naar reproduser-endepunkt treffes fra ekstern IP`() {
        every { isInternal(any()) } returns false

        val response = runInTestContext { httpClient ->
            httpClient.post("/internal/tiltakshendelse/reproduser/${UUID.randomUUID()}")
        }
        response.status shouldBe HttpStatusCode.Forbidden

        coVerify { mockTiltakshendelseService wasNot Called }
    }

    @Test
    fun `skal returnere 200 OK og reprodusere hendelse naar reproduser-endepunkt treffes fra intern IP`() {
        every { isInternal(any()) } returns true

        val id = UUID.randomUUID()

        val response = runInTestContext { httpClient ->
            httpClient.post("/internal/tiltakshendelse/reproduser/$id")
        }
        response.status shouldBe HttpStatusCode.OK

        coVerify { mockTiltakshendelseService.reproduser(id) }
    }

    @Test
    fun `isInternal skal returnere false for ekstern IP`() {
        isInternal("42.42.42.42") shouldBe false
    }

    @Test
    fun `isInternal skal returnere true for intern IP`() {
        isInternal("127.0.0.1") shouldBe true
    }
}
