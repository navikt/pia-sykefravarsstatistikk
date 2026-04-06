val arrowCoreVersion = "2.2.2.1"
val iaFellesVersion = "2.0.4"
val flywayPostgresqlVersion = "12.2.0"
val kafkaClientVersion = "4.2.0"
val kotestVersion = "6.1.10"
val kotlinVersion = "2.3.20" // OBS: CodeQL støtter ikke Kotlin > 2.3.0
val ktorVersion = "3.4.2"
val logbackVersion = "1.5.32"
val logstashLogbackEncoderVersion = "9.0"
val mockOAuth2ServerVersion = "3.0.1"
val mockServerVersion = "2.0.4"
val nimbusJoseJwtVersion = "10.8"
val prometheusVersion = "1.16.4"
val testcontainersVersion = "2.0.4"
val testcontainersFakeGCSVersion = "0.3.0"
val testcontainersKafkaVersion = "1.21.4"
val testcontainersPostgresqlVersion = "1.21.4"
val opentelemetryLogbackMdcVersion = "2.26.1-alpha"

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("application")
}

application {
    mainClass.set("no.nav.pia.sykefravarsstatistikk.ApplicationKt")
    applicationName = "app"
}

tasks {
    test {
        dependsOn(installDist)
    }
}

group = "no.nav"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.nimbusds:nimbus-jose-jwt:$nimbusJoseJwtVersion")
    implementation("io.arrow-kt:arrow-core:$arrowCoreVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer-jvm:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$prometheusVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.6.x-compat")
    // Logging (inkl. auditlogg)
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:$opentelemetryLogbackMdcVersion")
    // Kafka
    implementation("at.yawk.lz4:lz4-java:1.10.4")
    implementation("org.apache.kafka:kafka-clients:$kafkaClientVersion") {
        // "Fikser CVE-2025-12183 - lz4-java >1.8.1 har sårbar versjon (transitive dependency fra kafka-clients:4.1.0)"
        exclude("org.lz4", "lz4-java")
    }

    // Database
    implementation("org.postgresql:postgresql:42.7.10")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayPostgresqlVersion")
    implementation("com.github.seratch:kotliquery:1.9.1")

    // Felles definisjoner for IA-domenet
    implementation("com.github.navikt:ia-felles:$iaFellesVersion")

    // Test
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersKafkaVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion")
    testImplementation("software.xdev.mockserver:testcontainers:$mockServerVersion")
    testImplementation("software.xdev.mockserver:client:$mockServerVersion")
    testImplementation("io.aiven:testcontainers-fake-gcs-server:$testcontainersFakeGCSVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")

    constraints {
        implementation("com.fasterxml.jackson.core:jackson-core") {
            version { require("2.21.1") }
            because("versjoner < 2.21.1 har sårbarhet. inkludert i ktor-server-auth:3.4.0")
        }
        implementation("tools.jackson.core:jackson-core") {
            version { require("3.1.0") }
            because("versjoner < 3.1.0 har sårbarhet. inkludert i logstash-logback-encoder:9.0")
        }
    }
}
