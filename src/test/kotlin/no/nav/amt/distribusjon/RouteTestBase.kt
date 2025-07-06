package no.nav.amt.distribusjon

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.amt.distribusjon.application.isReadyKey
import no.nav.amt.distribusjon.application.plugins.applicationConfig
import no.nav.amt.distribusjon.application.plugins.configureAuthentication
import no.nav.amt.distribusjon.application.plugins.configureRouting
import no.nav.amt.distribusjon.application.plugins.configureSerialization
import no.nav.amt.distribusjon.digitalbruker.DigitalBrukerService
import no.nav.amt.distribusjon.tiltakshendelse.TiltakshendelseService
import org.junit.Before

abstract class RouteTestBase {
    val mockDigitalBrukerService: DigitalBrukerService = mockk(relaxed = true)
    val mockTiltakshendelseService: TiltakshendelseService = mockk(relaxed = true)

    @Before
    fun setup() {
        clearAllMocks()
    }

    fun <T : Any> runInTestContext(appIsReady: Boolean = true, block: suspend (HttpClient) -> T): T {
        lateinit var result: T

        testApplication {
            application {
                if (appIsReady) attributes.put(isReadyKey, true)

                configureSerialization()
                configureAuthentication(testEnvironment)
                configureRouting(mockDigitalBrukerService, mockTiltakshendelseService)
            }

            result =
                block(
                    createClient {
                        install(ContentNegotiation) {
                            jackson { applicationConfig() }
                        }
                    },
                )
        }

        return result
    }
}
