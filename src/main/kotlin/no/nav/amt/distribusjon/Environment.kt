package no.nav.amt.distribusjon

data class Environment(
    val dbUsername: String = getEnvVar(DB_USERNAME_KEY),
    val dbPassword: String = getEnvVar(DB_PASSWORD_KEY),
    val dbDatabase: String = getEnvVar(DB_DATABASE_KEY),
    val dbHost: String = getEnvVar(DB_HOST_KEY),
    val dbPort: String = getEnvVar(DB_PORT_KEY),
    val azureAdTokenUrl: String = getEnvVar(AZURE_AD_TOKEN_URL_KEY),
    val azureClientId: String = getEnvVar(AZURE_APP_CLIENT_ID_KEY),
    val azureClientSecret: String = getEnvVar(AZURE_APP_CLIENT_SECRET_KEY),
    val azureJwkKeysUrl: String = getEnvVar(AZURE_OPENID_CONFIG_JWKS_URI_KEY),
    val azureJwtIssuer: String = getEnvVar(AZURE_OPENID_CONFIG_ISSUER_KEY),
    val amtPdfgenUrl: String = getEnvVar(AMT_PDFGEN_URL_KEY),
    val amtPersonUrl: String = getEnvVar(AMT_PERSONSERVICE_URL_KEY),
    val amtPersonScope: String = getEnvVar(AMT_PERSONSERVICE_SCOPE_KEY),
) {
    companion object {
        const val DB_USERNAME_KEY = "DB_USERNAME"
        const val DB_PASSWORD_KEY = "DB_PASSWORD"
        const val DB_DATABASE_KEY = "DB_DATABASE"
        const val DB_HOST_KEY = "DB_HOST"
        const val DB_PORT_KEY = "DB_PORT"

        const val KAFKA_CONSUMER_GROUP_ID = "amt-distribusjon-consumer"

        const val AZURE_AD_TOKEN_URL_KEY = "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"
        const val AZURE_APP_CLIENT_SECRET_KEY = "AZURE_APP_CLIENT_SECRET"
        const val AZURE_APP_CLIENT_ID_KEY = "AZURE_APP_CLIENT_ID"
        const val AZURE_OPENID_CONFIG_JWKS_URI_KEY = "AZURE_OPENID_CONFIG_JWKS_URI"
        const val AZURE_OPENID_CONFIG_ISSUER_KEY = "AZURE_OPENID_CONFIG_ISSUER"

        const val AMT_PDFGEN_URL_KEY = "AMT_PDFGEN"
        const val AMT_PERSONSERVICE_SCOPE_KEY = "AMT_PERSONSERVICE_SCOPE"
        const val AMT_PERSONSERVICE_URL_KEY = "AMT_PERSONSERVICE_URL"

        const val HTTP_CLIENT_TIMEOUT_MS = 10_000

        const val DELTAKER_HENDELSE_TOPIC = "amt.deltaker-hendelse-v1"
        const val MINSIDE_VARSEL_TOPIC = "min-side.aapen-brukervarsel-v1"
        const val MINSIDE_VARSEL_HENDELSE_TOPIC = "min-side.aapen-varsel-hendelse-v1"

        val cluster: String = getEnvVar("NAIS_CLUSTER_NAME", "lokal")
        val appName: String = getEnvVar("NAIS_APP_NAME", "amt-distribusjon")
        val namespace: String = getEnvVar("NAIS_NAMESPACE", "amt")

        val testContainersReuse = getEnvVar("TESTCONTAINERS_REUSE", "false").toBoolean()

        val unleashUrl = getEnvVar("UNLEASH_SERVER_API_URL")
        val unleashToken = getEnvVar("UNLEASH_SERVER_API_TOKEN")

        const val VARSEL_TOGGLE = "amt.minside-varsel"

        fun isDev(): Boolean {
            return cluster == "dev-gcp"
        }

        fun isProd(): Boolean {
            return cluster == "prod-gcp"
        }

        fun isLocal(): Boolean {
            return !isDev() && !isProd()
        }
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) = System.getenv(varName)
    ?: System.getProperty(varName)
    ?: defaultValue
    ?: if (Environment.isLocal()) "" else error("Missing required variable $varName")
