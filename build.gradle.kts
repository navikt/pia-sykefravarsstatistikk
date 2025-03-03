val altinnKlientVersion = "5.0.0"
val arrowCoreVersion = "2.0.1"
val kafkClientVersion = "3.9.0"
val kotestVersion = "6.0.0.M1"
val kotlinVersion = "2.1.10"
val ktorVersion = "3.1.0"
val logbackVersion = "1.5.16"
val iaFellesVersion = "1.10.2"
val logstashLogbackEncoderVersion = "8.0"
val mockOAuth2ServerVersion = "2.1.10"
val nimbusJoseJwtVersion = "10.0.1"
val prometeusVersion = "1.14.4"
val testcontainersVersion = "1.20.4"
val testMockServerVersion = "5.15.0"
val wiremockStandaloneVersion = "3.12.0"

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "no.nav"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.navikt:altinn-rettigheter-proxy-klient:altinn-rettigheter-proxy-klient-$altinnKlientVersion")
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
    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkClientVersion")

    // Database
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.3.3")
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
    testImplementation("org.testcontainers:mockserver:$testcontainersVersion")
    testImplementation("org.mock-server:mockserver-client-java:$testMockServerVersion")
    // Warning:(73, 24)  Provides transitive vulnerable dependency maven:com.google.guava:guava:31.1-jre CVE-2023-2976 5.5 Files or Directories Accessible to External Parties  Results powered by Mend.io
// Warning:(73, 24)  Provides transitive vulnerable dependency maven:org.bouncycastle:bcprov-jdk18on:1.72 CVE-2024-29857 7.5 Uncontrolled Resource Consumption ('Resource Exhaustion') CVE-2024-30172 7.5 Loop with Unreachable Exit Condition ('Infinite Loop') CVE-2024-30171 5.9 Observable Discrepancy CVE-2023-33202 5.5 Uncontrolled Resource Consumption ('Resource Exhaustion') CVE-2023-33201 5.3 Improper Certificate Validation  Results powered by Mend.io
    // Warning:(73, 24)  Provides transitive vulnerable dependency maven:org.xmlunit:xmlunit-core:2.9.1 CVE-2024-31573 5.6 Insufficient Information  Results powered by Mend.io

    testImplementation("io.aiven:testcontainers-fake-gcs-server:0.2.0")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockStandaloneVersion")
    testImplementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")

    constraints {
        implementation("commons-codec:commons-codec") {
            version {
                require("1.18.0")
            }
            because(
                "altinn-rettigheter-proxy bruker codec 1.11 som har en sårbarhet",
            )
        }
        implementation("net.minidev:json-smart") {
            version {
                require("2.5.2")
            }
            because(
                "From Kotlin version: 1.7.20 -> Earlier versions of json-smart package are vulnerable to Denial of Service (DoS) due to a StackOverflowError when parsing a deeply nested JSON array or object.",
            )
        }
        implementation("io.netty:netty-codec-http2") {
            version {
                require("4.1.118.Final")
            }
            because("From Ktor version: 2.3.5 -> io.netty:netty-codec-http2 vulnerable to HTTP/2 Rapid Reset Attack")
        }
        testImplementation("com.google.guava:guava") {
            version {
                require("33.3.1-jre")
            }
            because("Mockserver har sårbar versjon")
        }
        testImplementation("org.bouncycastle:bcprov-jdk18on") {
            version {
                require("1.80")
            }
            because("bcprov-jdk18on har sårbar versjon")
        }
        testImplementation("org.xmlunit:xmlunit-core") {
            version {
                require("2.10")
            }
            because("xmlunit-core har sårbar versjon")
        }
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
