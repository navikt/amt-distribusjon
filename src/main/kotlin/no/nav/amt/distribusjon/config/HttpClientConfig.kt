package no.nav.amt.distribusjon.config

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration(proxyBeanMethods = false)
@EnableOAuth2Client(cacheEnabled = true)
class HttpClientConfig(
    private val restClientBuilder: RestClient.Builder,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    private val clientConfigurationProperties: ClientConfigurationProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun deltakerHttpClient(
        @Value($$"${app.amt-deltaker-url}") amtDeltakerUrl: String,
    ): RestClient = buildRestClient(amtDeltakerUrl, AMT_DELTAKER_AAD)

    @Bean
    fun dokDistKanalHttpClient(
        @Value($$"${app.dok-dist-kanal-url}") dokDistKanalUrl: String,
    ): RestClient = buildRestClient(dokDistKanalUrl, DOK_DIST_KANAL_AAD)

    @Bean
    fun dokArkivHttpClient(
        @Value($$"${app.dok-arkiv-url}") dokArkivUrl: String,
    ): RestClient = buildRestClient(dokArkivUrl, DOK_ARKIV_AAD)

    @Bean
    fun dokDistFordelingHttpClient(
        @Value($$"${app.dok-dist-fordeling-url}") dokDistFordelingUrl: String,
    ): RestClient = buildRestClient(dokDistFordelingUrl, DOK_DIST_FORDELING_AAD)

    @Bean
    fun pdfGenHttpClient(
        @Value($$"${app.pdf-gen-url}") pdfGenUrl: String,
    ): RestClient = buildRestClient(pdfGenUrl, PDF_GEN_AAD)

    @Bean
    fun personServiceHttpClient(
        @Value($$"${app.person-service-url}") personUrl: String,
    ): RestClient = buildRestClient(personUrl, AMT_PERSON_AAD)

    @Bean
    fun veilarboHttpClient(
        @Value($$"${app.veilarbo-url}") veilarboUrl: String,
    ): RestClient = buildRestClient(veilarboUrl, VEILARBO_AAD)

    private fun buildRestClient(baseUrl: String, registrationName: String) = restClientBuilder
        .baseUrl(baseUrl)
        .defaultHeaders { headers ->
            headers.accept = listOf(MediaType.APPLICATION_JSON)
        }.requestInterceptor { request, body, execution ->
            log.debug("Setting Bearer token for $registrationName")
            request.headers.setBearerAuth(getToken(registrationName))
            execution.execute(request, body)
        }.build()

    private fun getToken(registrationName: String): String = oAuth2AccessTokenService
        .getAccessToken(getClientProperties(registrationName))
        .access_token
        ?: error("Kunne ikke hente token for $registrationName")

    private fun getClientProperties(registrationName: String) = clientConfigurationProperties.registration[registrationName]
        ?: throw IllegalArgumentException("Fant ikke config for $registrationName")

    companion object {
        const val AMT_DELTAKER_AAD = "amt-deltaker-aad"
        const val DOK_DIST_KANAL_AAD = "dok-dist-kanal-aad"
        const val DOK_ARKIV_AAD = "dok-arkiv-aad"
        const val DOK_DIST_FORDELING_AAD = "dok-dist-fordeling-aad"
        const val PDF_GEN_AAD = "pdf-gen-aad"
        const val AMT_PERSON_AAD = "amt-person-aad"
        const val VEILARBO_AAD = "veilarboppfolging-aad"
    }
}
