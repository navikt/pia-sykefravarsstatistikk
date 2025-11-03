val arrowCoreVersion = "2.2.0"
val iaFellesVersion = "2.0.3"
val flywayPostgresqlVersion = "11.15.0"
val kafkClientVersion = "4.1.0"
val kotestVersion = "6.0.4"
val kotlinVersion = "2.2.21"
val ktorVersion = "3.3.1"
val logbackVersion = "1.5.20"
val logstashLogbackEncoderVersion = "8.1"
val mockOAuth2ServerVersion = "3.0.0"
val mockServerVersion = "1.3.0"
val nimbusJoseJwtVersion = "10.5"
val prometheusVersion = "1.15.5"
val testcontainersVersion = "1.21.3"
val testcontainersFakeGCSVersion = "0.2.0"
val opentelemetryLogbackMdcVersion = "2.16.0-alpha"

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("application")
}

application {
    mainClass.set("no.nav.pia.sykefravarsstatistikk.ApplicationKt")
    applicationName = "app"
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    // Logging (inkl. auditlogg)
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:$opentelemetryLogbackMdcVersion")
    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkClientVersion")

    // Database
    implementation("org.postgresql:postgresql:42.7.8")
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
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("software.xdev.mockserver:testcontainers:$mockServerVersion")
    testImplementation("software.xdev.mockserver:client:$mockServerVersion")
    testImplementation("io.aiven:testcontainers-fake-gcs-server:$testcontainersFakeGCSVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")

    constraints {
        implementation("io.netty:netty-codec-http2") {
            version {
                require("4.2.7.Final")
            }
            because(
                "ktor-server-netty har sårbar versjon",
            )
        }

        testImplementation("org.apache.commons:commons-compress") {
            version {
                require("1.28.0")
            }
            because("testcontainers har sårbar versjon")
        }
        testImplementation("com.jayway.jsonpath:json-path") {
            version {
                require("2.10.0")
            }
            because(
                """
                json-path v2.8.0 was discovered to contain a stack overflow via the Criteria.parse() method.
                introdusert gjennom io.kotest:kotest-assertions-json:5.8.0
                """.trimIndent(),
            )
        }
    }
}
