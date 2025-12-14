package no.nav.amt.distribusjon

data class Environment(
    val leaderElectorUrl: String = getEnvVar(LEADER_ELECTOR_URL),
) {
    companion object {
        const val KAFKA_CONSUMER_GROUP_ID = "amt-distribusjon-consumer"

        const val LEADER_ELECTOR_URL = "ELECTOR_GET_URL"
        const val SAF_TEMA = "OPP"

        const val DELTAKER_HENDELSE_TOPIC = "amt.deltaker-hendelse-v1"
        const val MINSIDE_VARSEL_TOPIC = "min-side.aapen-brukervarsel-v1"
        const val MINSIDE_VARSEL_HENDELSE_TOPIC = "min-side.aapen-varsel-hendelse-v1"
        const val TILTAKSHENDELSE_TOPIC = "obo.tiltakshendelse-v1"
        const val ARRANGOR_MELDING_TOPIC = "amt.arrangor-melding-v1"

        val cluster: String = getEnvVar("NAIS_CLUSTER_NAME", "lokal")
        val appName: String = getEnvVar("NAIS_APP_NAME", "amt-distribusjon")
        val namespace: String = getEnvVar("NAIS_NAMESPACE", "amt")

        fun isDev(): Boolean = cluster == "dev-gcp"

        fun isProd(): Boolean = cluster == "prod-gcp"

        fun isLocal(): Boolean = !isDev() && !isProd()
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) = System.getenv(varName)
    ?: System.getProperty(varName)
    ?: defaultValue
    ?: if (Environment.isLocal()) "" else error("Missing required variable $varName")
