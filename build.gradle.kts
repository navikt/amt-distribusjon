group = "no.nav.amt-distribusjon"
version = "1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "2.2.21"

    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"

    id("io.ktor.plugin") version "3.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    application
    distribution
}

repositories {
    mavenCentral()
    maven { setUrl("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") }
}

val tokenSupportVersion = "6.0.0"
val jacksonModuleKotlinVersion = "3.0.3"
val springmockkVersion = "5.0.1"

val ktorVersion = "3.3.3"
val logbackVersion = "1.5.21"
val prometeusVersion = "1.16.0"
val ktlintVersion = "1.6.0"
val jacksonVersion = "2.20.1"
val logstashEncoderVersion = "9.0"
val commonVersion = "3.2025.10.10_08.21-bb7c7830d93c"
val kotestVersion = "6.0.7"
val flywayVersion = "11.18.0"
val hikariVersion = "7.0.2"
val kotliqueryVersion = "1.9.1"
val postgresVersion = "42.7.8"
val caffeineVersion = "3.2.3"
val unleashVersion = "11.1.1"
val nimbusVersion = "10.6"
val amtLibVersion = "1.2025.12.11_11.16-62287e250f37"

// fjernes ved neste release av org.apache.kafka:kafka-clients
configurations.configureEach {
    resolutionStrategy {
        capabilitiesResolution {
            withCapability("org.lz4:lz4-java") {
                select(candidates.first { (it.id as ModuleComponentIdentifier).group == "at.yawk.lz4" })
            }
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.springframework.boot:spring-boot-kafka")

    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")

    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")

    implementation("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-data-jdbc-test")
    testImplementation("org.springframework.boot:spring-boot-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")

    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json-jvm:$kotestVersion")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")

    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-kafka")

    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")

    // orig from here

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
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")
    implementation("com.github.ben-manes.caffeine:caffeine")

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
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
            "-Xwarning-level=IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE:disabled",
        )
        // jvmTarget = JvmTarget.JVM_21
    }
}

ktlint {
    version = ktlintVersion
}

tasks.named<Test>("test") {
    useJUnitPlatform()

    jvmArgs(
        "-Xshare:off",
        "-XX:+EnableDynamicAgentLoading",
    )
}

tasks.named<Jar>("jar") {
    enabled = false
}
