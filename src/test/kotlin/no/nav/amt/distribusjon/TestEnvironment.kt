package no.nav.amt.distribusjon

import java.nio.file.Paths

val testEnvironment = Environment()

fun getAzureJwkKeysUrl(): String {
    val path = "src/test/resources/jwkset.json"
    return Paths
        .get(path)
        .toUri()
        .toURL()
        .toString()
}
