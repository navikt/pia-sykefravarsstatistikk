package no.nav.pia.sykefravarsstatistikk.helper

import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.time.withTimeoutOrNull
import kotlinx.datetime.LocalDateTime
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.helper.AuthContainerHelper.Companion.FNR
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.images.builder.ImageFromDockerfile
import java.time.Duration
import kotlin.io.path.Path
import kotlin.test.fail

class TestContainerHelper {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TestContainerHelper::class.java)
        val network = Network.newNetwork()
        val altinnTilgangerContainerHelper = AltinnTilgangerContainerHelper(network = network)
        val authContainerHelper = AuthContainerHelper(network = network)
        val postgresContainerHelper = PostgrestContainerHelper(network = network, log = log)
        val kafkaContainerHelper = KafkaContainerHelper(network = network, log = log)
        private val wiremockContainerHelper = WiremockContainerHelper()

        // Setter lokal dato for å kunne teste /publiseringsdato uten å være avhengig av tidspunktet testen kjører
        private val lokalDato = LocalDateTime.parse("2025-03-01T15:59:59")

        val applikasjon: GenericContainer<*> = GenericContainer(
            ImageFromDockerfile().withDockerfile(Path("./Dockerfile")),
        ).dependsOn(
            altinnTilgangerContainerHelper.altinnTilgangerContainer,
            kafkaContainerHelper.kafkaContainer,
            postgresContainerHelper.postgresContainer,
            authContainerHelper.authContainer,
        ).withNetwork(network).withExposedPorts(8080).withLogConsumer(
            Slf4jLogConsumer(log).withPrefix("pia.sykefravarsstatistikk").withSeparateOutputStreams(),
        ).withEnv(
            mapOf(
                "CONSUMER_LOOP_DELAY" to "1",
                "NAIS_CLUSTER_NAME" to "lokal",
                "LOKAL_DATO" to lokalDato.toString(),
                "ALTINN_RETTIGHET_SERVICE_CODE" to "3403",
                "ALTINN_RETTIGHET_SERVICE_EDITION" to "1",
            )
                .plus(postgresContainerHelper.envVars())
                .plus(kafkaContainerHelper.envVars())
                .plus(wiremockContainerHelper.envVars())
                .plus(altinnTilgangerContainerHelper.envVars())
                .plus(authContainerHelper.envVars()),
        ).waitingFor(HttpWaitStrategy().forPath("/internal/isalive").withStartupTimeout(Duration.ofSeconds(20)))
            .apply {
                start()
            }

        internal suspend fun GenericContainer<*>.performGet(
            url: String,
            config: HttpRequestBuilder.() -> Unit = {},
        ) = performRequest(url) {
            config()
            method = HttpMethod.Get
        }

        suspend fun hentAggregertStatistikk(
            orgnr: String,
            config: HttpRequestBuilder.() -> Unit = {},
        ): AggregertStatistikkResponseDto =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/$orgnr/siste4kvartaler/aggregert",
                config = config,
            )?.let { response ->
                if (response.status != HttpStatusCode.OK) {
                    fail("Feil ved henting av aggregert statistikk, status: ${response.status}, body: ${response.bodyAsText()}")
                }
                response.body()
            } ?: fail("Feil ved henting av aggregert statistikk, mottok ikke respons")

        suspend fun hentAggregertStatistikkResponse(
            orgnr: String,
            config: HttpRequestBuilder.() -> Unit = {},
        ): HttpResponse =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/$orgnr/siste4kvartaler/aggregert",
                config = config,
            ) ?: fail("Feil ved henting av aggregert statistikk, mottok ikke respons")

        suspend fun hentKvartalsvisStatistikk(
            orgnr: String,
            config: HttpRequestBuilder.() -> Unit = {},
        ): List<KvartalsvisSykefraværshistorikkDto> =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/$orgnr/historikk/kvartalsvis",
                config = config,
            )?.let { response ->
                if (response.status != HttpStatusCode.OK) {
                    fail("Feil ved henting av kvartalsvis statistikk, status: ${response.status}, body: ${response.bodyAsText()}")
                }
                response.body()
            } ?: fail("Feil ved henting av kvartalsvis statistikk, mottok ikke respons")

        suspend fun hentKvartalsvisStatistikkResponse(
            orgnr: String,
            config: HttpRequestBuilder.() -> Unit = {},
        ): HttpResponse =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/$orgnr/historikk/kvartalsvis",
                config = config,
            ) ?: fail("Feil ved henting av kvartalsvis statistikk, mottok ikke respons")

        suspend fun hentPubliseringsdatoResponse(config: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/publiseringsdato",
                config = config,
            ) ?: fail("Feil ved henting av publiseringsdato, mottok ikke respons")

        suspend fun hentOrganisasjonerTilgangResponse(config: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/organisasjoner/tilgang",
                config = config,
            ) ?: fail("Feil ved henting av organisasjoner vedkommenede har tilgang til, mottok ikke respons")

        suspend fun hentOrganisasjonerMedEnkeltrettighetResponse(config: HttpRequestBuilder.() -> Unit = {}): HttpResponse =
            applikasjon.performGet(
                url = "/sykefravarsstatistikk/organisasjoner/enkeltrettighet",
                config = config,
            ) ?: fail("Feil ved henting av organisasjoner på enkelrettighet, mottok ikke respons")

        private val httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
        }

        private suspend fun GenericContainer<*>.performRequest(
            url: String,
            config: HttpRequestBuilder.() -> Unit = {},
        ) = withTimeoutOrNull(Duration.ofSeconds(5)) {
            httpClient.request {
                config()
                header(HttpHeaders.Accept, "application/json")
                url {
                    protocol = URLProtocol.HTTP
                    host = this@performRequest.host
                    port = firstMappedPort
                    path(url)
                }
            }
        }

        infix fun GenericContainer<*>.shouldContainLog(regex: Regex) = logs shouldContain regex

        internal fun accessToken(
            subject: String = FNR,
            audience: String = "hei",
            claims: Map<String, String> = mapOf(
                "acr" to "Level4",
                "pid" to subject,
            ),
        ) = authContainerHelper.issueToken(
            subject = subject,
            audience = audience,
            claims = claims,
        )
    }
}
