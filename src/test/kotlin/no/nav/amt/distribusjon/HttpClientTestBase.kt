package no.nav.amt.distribusjon

import com.nimbusds.oauth2.sdk.GrantType
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod.CLIENT_SECRET_BASIC
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.amt.distribusjon.config.HttpClientConfig
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.AMT_DELTAKER_AAD
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.AMT_PERSON_AAD
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.DOK_ARKIV_AAD
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.DOK_DIST_FORDELING_AAD
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.DOK_DIST_KANAL_AAD
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.PDF_GEN_AAD
import no.nav.amt.distribusjon.config.HttpClientConfig.Companion.VEILARBO_AAD
import no.nav.security.token.support.client.core.ClientAuthenticationProperties
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.client.MockRestServiceServer
import tools.jackson.databind.ObjectMapper
import java.net.URI

@ActiveProfiles("test")
@EnableMockOAuth2Server
@Import(HttpClientConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
abstract class HttpClientTestBase {
    @Autowired
    protected lateinit var mockServer: MockRestServiceServer

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @MockkBean
    protected lateinit var clientConfigurationProperties: ClientConfigurationProperties

    @MockkBean
    protected lateinit var oAuth2AccessTokenService: OAuth2AccessTokenService

    @BeforeEach
    fun baseSetup() {
        every { clientConfigurationProperties.registration } returns clientPropertiesInTest
        every { oAuth2AccessTokenService.getAccessToken(any()) } returns dummyToken
    }

    companion object {
        const val MOCKED_TOKEN = "mocked-token"

        private val dummyToken = OAuth2AccessTokenResponse(
            access_token = MOCKED_TOKEN,
            expires_in = 3600,
        )

        private val clientPropertiesInTest = setOf(
            AMT_DELTAKER_AAD,
            DOK_DIST_KANAL_AAD,
            DOK_ARKIV_AAD,
            DOK_DIST_FORDELING_AAD,
            PDF_GEN_AAD,
            AMT_PERSON_AAD,
            VEILARBO_AAD,
        ).associateWith {
            ClientProperties(
                tokenEndpointUrl = URI.create("https://fake-token-endpoint"),
                grantType = GrantType.CLIENT_CREDENTIALS,
                authentication = ClientAuthenticationProperties(
                    clientId = "fake-client-id",
                    clientSecret = "fake-secret",
                    clientAuthMethod = CLIENT_SECRET_BASIC,
                ),
            )
        }
    }
}
