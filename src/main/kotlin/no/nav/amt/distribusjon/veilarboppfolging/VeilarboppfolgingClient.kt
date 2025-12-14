package no.nav.amt.distribusjon.veilarboppfolging

import no.nav.amt.distribusjon.AppConstants.NAV_CONSUMER_ID_HEADER_KEY
import no.nav.amt.distribusjon.config.CacheConfig.Companion.MANUELL_OPPFOLGING_CACHE
import no.nav.amt.distribusjon.exchangeWithLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@Service
class VeilarboppfolgingClient(
    private val veilarboHttpClient: RestClient,
    @Value($$"${app.app-name}") private val applicationName: String,
) {
    fun opprettEllerHentSak(oppfolgingsperiodeId: UUID): Sak = veilarboHttpClient
        .post()
        .uri("/veilarboppfolging/api/v3/sak/$oppfolgingsperiodeId")
        .header(NAV_CONSUMER_ID_HEADER_KEY, applicationName)
        .exchangeWithLogging("Kunne ikke hente sak fra veilarboppfolging for oppfolgingsperiode $oppfolgingsperiodeId")

    @Cacheable(MANUELL_OPPFOLGING_CACHE)
    fun erUnderManuellOppfolging(personident: String): Boolean = veilarboHttpClient
        .post()
        .uri("/veilarboppfolging/api/v3/hent-manuell")
        .header(NAV_CONSUMER_ID_HEADER_KEY, applicationName)
        .body(ManuellStatusRequest(personident))
        .exchangeWithLogging<ManuellV2Response>("Kunne ikke hente manuell oppfølging fra veilarboppfolging")
        .erUnderManuellOppfolging
}

data class Sak(
    val oppfolgingsperiodeId: UUID,
    val sakId: Long,
    val fagsaksystem: String,
)

data class ManuellStatusRequest(
    val fnr: String,
)

data class ManuellV2Response(
    val erUnderManuellOppfolging: Boolean,
)
