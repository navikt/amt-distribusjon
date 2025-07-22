group = "no.nav.amt-distribusjon"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "2.2.0"

    kotlin("jvm") version kotlinVersion
    id("io.ktor.plugin") version "3.2.2"
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
    id("com.gradleup.shadow") version "8.3.8"
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

val ktorVersion = "3.2.2"
val logbackVersion = "1.5.18"
val prometeusVersion = "1.15.2"
val ktlintVersion = "1.2.1"
val jacksonVersion = "2.19.2"
val logstashEncoderVersion = "8.1"
val commonVersion = "3.2025.06.23_14.50-3af3985d8555"
val kotestVersion = "5.9.1"
val flywayVersion = "11.10.3"
val hikariVersion = "6.3.1"
val kotliqueryVersion = "1.9.1"
val postgresVersion = "42.7.7"
val caffeineVersion = "3.2.2"
val unleashVersion = "11.0.2"
val nimbusVersion = "10.4"
val amtLibVersion = "1.2025.07.21_09.55-93558d2bbc68"

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

    implementation("no.nav.tms.varsel:kotlin-builder:2.1.1")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("no.nav.common:log:$commonVersion")

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("io.getunleash:unleash-client-java:$unleashVersion")

    implementation("no.nav.amt.lib:kafka:$amtLibVersion")
    implementation("no.nav.amt.lib:utils:$amtLibVersion")
    implementation("no.nav.amt.lib:models:$amtLibVersion")
    implementation("no.nav.amt.lib:outbox:$amtLibVersion")

    testImplementation("no.nav.amt.lib:testing:$amtLibVersion")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusVersion")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    mainClass.set("no.nav.amt.distribusjon.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

ktlint {
    version = ktlintVersion
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
    )
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "no.nav.amt.distribusjon.ApplicationKt",
        )
    }
}

tasks.shadowJar {
    mergeServiceFiles()
}
