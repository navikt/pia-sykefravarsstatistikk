val arrowCoreVersion = "2.1.2"
val iaFellesVersion = "1.10.2"
val kafkClientVersion = "3.9.1"
val kotestVersion = "5.9.1"
val kotlinVersion = "2.2.0"
val ktorVersion = "3.2.0"
val logbackVersion = "1.5.18"
val logstashLogbackEncoderVersion = "8.1"
val mockOAuth2ServerVersion = "2.2.1"
val mockServerVersion = "1.0.19"
val nimbusJoseJwtVersion = "10.3.1"
val prometeusVersion = "1.15.1"
val testcontainersVersion = "1.21.3"
val testcontainersFakeGCSVersion = "0.2.0"
val opentelemetryLogbackMdcVersion = "2.16.0-alpha"

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("com.gradleup.shadow") version "8.3.5"
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
    implementation("io.micrometer:micrometer-registry-prometheus:$prometeusVersion")
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
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    // Logging (inkl. auditlogg)
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:$opentelemetryLogbackMdcVersion")
    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkClientVersion")

    // Database
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.10.0")
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
        testImplementation("org.apache.commons:commons-compress") {
            version {
                require("1.27.1")
            }
            because("testcontainers har sårbar versjon")
        }
        testImplementation("com.jayway.jsonpath:json-path") {
            version {
                require("2.9.0")
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

tasks {
    shadowJar {
        mergeServiceFiles()
        manifest {
            attributes("Main-Class" to "no.nav.pia.sykefravarsstatistikk.ApplicationKt")
        }
    }
    test {
        dependsOn(shadowJar)
    }
}
