package no.nav.pia.sykefravarsstatistikk.helper

import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.images.builder.ImageFromDockerfile
import java.time.Duration
import kotlin.io.path.Path

class TestContainerHelper {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TestContainerHelper::class.java)
        val network = Network.newNetwork()

        val postgresContainer = PostgrestContainerHelper(network = network, log = log)

        val kafkaContainerHelper = KafkaContainerHelper(network = network, log = log)

        val sykefraværsstatistikkApplikasjon =
            GenericContainer(
                ImageFromDockerfile().withDockerfile(Path("./Dockerfile")),
            )
                .dependsOn(
                    kafkaContainerHelper.kafkaContainer,
                    postgresContainer.postgresContainer,
                )
                .withNetwork(network)
                .withExposedPorts(8080)
                .withLogConsumer(
                    Slf4jLogConsumer(log).withPrefix("pia.sykefravarsstatistikk").withSeparateOutputStreams(),
                ).withEnv(
                    postgresContainer.envVars()
                        .plus(
                            kafkaContainerHelper.envVars()
                                .plus(
                                    mapOf(
                                        "CONSUMER_LOOP_DELAY" to "1",
                                        "NAIS_CLUSTER_NAME" to "lokal",
                                    ),
                                ),
                        ),
                )
                .waitingFor(HttpWaitStrategy().forPath("/internal/isalive").withStartupTimeout(Duration.ofSeconds(20)))
                .apply {
                    start()
                }

        infix fun GenericContainer<*>.shouldContainLog(regex: Regex) = logs shouldContain regex
    }
}

private val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

private suspend fun GenericContainer<*>.performRequest(
    url: String,
    config: HttpRequestBuilder.() -> Unit = {},
) = httpClient.request {
    config()
    header(HttpHeaders.Accept, "application/json")
    url {
        protocol = URLProtocol.HTTP
        host = this@performRequest.host
        port = firstMappedPort
        path(url)
    }
}

internal suspend fun GenericContainer<*>.performGet(
    url: String,
    config: HttpRequestBuilder.() -> Unit = {},
) = performRequest(url) {
    config()
    method = HttpMethod.Get
}
