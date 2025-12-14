package no.nav.amt.distribusjon.digitalbruker.api

import org.junit.jupiter.api.Disabled

@Disabled("Fix me")
class DigitalBrukerApiTest {
/*    @Test
    fun `skal teste autentisering - mangler token - returnerer 401`() = integrationTest { _, client ->
        client.post("/digital") { setBody("foo") }.status shouldBe HttpStatusCode.Unauthorized
    }

    @Test
    fun `post digital - ikke manuell oppfolging, SDP - bruker er digital`() = integrationTest { _, client ->
        val personident = randomIdent()
        MockResponseHandler.addDistribusjonskanalResponse(personident, Distribusjonskanal.SDP)
        MockResponseHandler.addManuellOppfolgingResponse(personident, false)
        client.post("/digital") { postRequest(DigitalBrukerRequest(personident)) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe staticObjectMapper.writeValueAsString(DigitalBrukerResponse(true))
        }
    }

    @Test
    fun `post digital - ikke manuell oppfolging, print - bruker er ikke digital`() = integrationTest { _, client ->
        val personident = randomIdent()
        MockResponseHandler.addDistribusjonskanalResponse(personident, Distribusjonskanal.PRINT)
        MockResponseHandler.addManuellOppfolgingResponse(personident, false)
        client.post("/digital") { postRequest(DigitalBrukerRequest(personident)) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe staticObjectMapper.writeValueAsString(DigitalBrukerResponse(false))
        }
    }

    @Test
    fun `post digital - manuell oppfolging, SDP - bruker er ikke digital`() = integrationTest { _, client ->
        val personident = randomIdent()
        MockResponseHandler.addDistribusjonskanalResponse(personident, Distribusjonskanal.SDP)
        MockResponseHandler.addManuellOppfolgingResponse(personident, true)
        client.post("/digital") { postRequest(DigitalBrukerRequest(personident)) }.apply {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe staticObjectMapper.writeValueAsString(DigitalBrukerResponse(false))
        }
    }

    @Test
    fun `post digital - veilarboppfolging feiler - returnerer 500`() = integrationTest { _, client ->
        val personident = randomIdent()
        MockResponseHandler.addDistribusjonskanalResponse(personident, Distribusjonskanal.SDP)
        MockResponseHandler.addManuellOppfolgingErrorResponse(personident, HttpStatusCode.InternalServerError)
        client.post("/digital") { postRequest(DigitalBrukerRequest(personident)) }.apply {
            status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `post digital - dokdistkanal feiler - returnerer 500`() = integrationTest { _, client ->
        val personident = randomIdent()
        MockResponseHandler.addDistribusjonskanalErrorResponse(personident, HttpStatusCode.InternalServerError)
        MockResponseHandler.addManuellOppfolgingResponse(personident, false)
        client.post("/digital") { postRequest(DigitalBrukerRequest(personident)) }.apply {
            status shouldBe HttpStatusCode.InternalServerError
        }
    }*/
}
