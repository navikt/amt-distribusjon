package no.nav.amt.distribusjon.application.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.jwt.JWTCredential
import no.nav.amt.distribusjon.Environment

fun Application.configureAuthentication(environment: Environment) {
/*
    val jwkProvider = JwkProviderBuilder(URI(environment.azureJwkKeysUrl).toURL())
        .cached(5, 12, TimeUnit.HOURS)
        .build()

    install(Authentication) {
        jwt("SYSTEM") {
            verifier(jwkProvider, environment.azureJwtIssuer) {
                withAudience(environment.azureClientId)
            }

            validate { credentials ->
                if (!erMaskinTilMaskin(credentials)) {
                    application.log.warn("Token med sluttbrukerkontekst har ikke tilgang til api med systemkontekst")
                    return@validate null
                }
                JWTPrincipal(credentials.payload)
            }
        }
    }
*/
}

fun erMaskinTilMaskin(credentials: JWTCredential): Boolean {
    val sub: String = credentials.payload.getClaim("sub").asString()
    val oid: String = credentials.payload.getClaim("oid").asString()
    return sub == oid
}
