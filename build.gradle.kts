import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

group = "no.nav.amt-distribusjon"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "2.1.10"

    kotlin("jvm") version kotlinVersion
    id("io.ktor.plugin") version "3.1.0"
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("com.gradleup.shadow") version "8.3.6"
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

val kotlinVersion = "2.1.0"
val ktorVersion = "3.1.0"
val logbackVersion = "1.5.16"
val prometeusVersion = "1.14.4"
val ktlintVersion = "1.2.1"
val jacksonVersion = "2.18.2"
val logstashEncoderVersion = "8.0"
val commonVersion = "3.2024.10.25_13.44-9db48a0dbe67"
val kafkaClientsVersion = "3.9.0"
val kotestVersion = "5.9.1"
val flywayVersion = "11.3.2"
val hikariVersion = "6.2.1"
val kotliqueryVersion = "1.9.1"
val postgresVersion = "42.7.5"
val caffeineVersion = "3.2.0"
val unleashVersion = "10.0.1"
val nimbusVersion = "10.0.1"
val amtLibVersion = "1.2025.02.12_12.23-94b6e1374ff8"

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-serialization-jackson-jvm")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-call-id-jvm")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")
    implementation("io.ktor:ktor-server-host-common-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    implementation("no.nav.tms.varsel:kotlin-builder:1.0.0")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("no.nav.common:log:$commonVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaClientsVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("io.getunleash:unleash-client-java:$unleashVersion")

    implementation("no.nav.amt.lib:kafka:$amtLibVersion")
    implementation("no.nav.amt.lib:utils:$amtLibVersion")
    implementation("no.nav.amt.lib:models:$amtLibVersion")

    testImplementation("no.nav.amt.lib:testing:$amtLibVersion")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("no.nav.amt.distribusjon.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "no.nav.amt.distribusjon.ApplicationKt",
        )
    }
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set(ktlintVersion)
}
