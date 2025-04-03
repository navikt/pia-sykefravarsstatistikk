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
            postgresContainerHelper.envVars().plus(kafkaContainerHelper.envVars())
                .plus(wiremockContainerHelper.envVars()).plus(
                    mapOf(
                        "CONSUMER_LOOP_DELAY" to "1",
                        "NAIS_CLUSTER_NAME" to "lokal",
                        "LOKAL_DATO" to lokalDato.toString(),
                    ),
                ).plus(
                    mapOf(
                        "TOKEN_X_CLIENT_ID" to "hei",
                        "TOKEN_X_ISSUER" to "http://authserver:6969/default",
                        "TOKEN_X_JWKS_URI" to "http://authserver:6969/default/jwks",
                        "TOKEN_X_TOKEN_ENDPOINT" to "http://authserver:6969/default/token",
                        "TOKEN_X_PRIVATE_JWK" to
                            """
                            {
                                "p": "1sKc9CQFXJ5q14wGjk6bAhIaWBiM2ZJHNCLcME0P60q_dNaC7osoj0-zDTwUWdiREIiI2y3DAArAGNlhyZqZwDNumL08_pM-ePXVoqiZWZ87Ch8g8csx27yU_AsDj6h64qRpV07x_TOzXRJdP5iQm_IO3qjyul9qlnXyd2X9h3c",
                                "kty": "RSA",
                                "q": "xkS_rKKUfowRYmHfha4birMJMvrRZEBmvOPs9gerRUyIy32R36UT5f2B8xwycExivtZpnlz-YgBrglIpWWXX1gUtgLb4dV_YQNE4rABQjWoa62NJeCeaL5mOoVJ-6Xx2mgt9Tb9JdZVyfQuC9-s74ImgKyYaN8y7LcW7EqxNa60",
                                "d": "TUr875CxdUBnuufXfGe9WELPlLE2N4tVtHO85qrVuwn41CueKKk92bF6mK4fFF_oIP6Ja22B96i7d-AY5GtLcwIJA_HNy6ndYJCWiMX9GlDJ7Y2TyYXrk4YXpZQWI3x18X7wbDs0JX1eVsxs2VWhjzyEsJfEbp0cyagBIZR_GE_WecEahhBUV2eGl9qf0qL50MnckFOZhQErEpyr0XPTfjqktwpmjZkTdONyvKoJhXhm7bngFQHl63RX3fIElsYFsvMYNpAH_I5NZg76Va79txrfR7X0diG6XZ4Kc5iUXXL1ZFnqgijVOzUYfldDikxaXc5wKPL5Jbs2GBe1fB14eQ",
                                "e": "AQAB",
                                "use": "sig",
                                "kid": "tokenx",
                                "qi": "zNeG8JxnjxSlCWbRv2sHwld6tf1OtDKTimo4VbNdOqmrm8sSUkuM9h0mrH0ZUbC7Q1n0Cp-4T_Q82QVzKXX71bGSolTI7c6NCTnzQXgTEylMaHgv-9MIG1N4raxWemlOt_0ZgdTjwDWNPXfbbx0oyc4NBJVZpQH_KEXKirAY5aI",
                                "dp": "Pbe8B2V6rP1R0xQIpkjsvxGYxIx5neUt1UvXX4Il-waGMvuasRcI1vaejEUhzBgyyD-UpPhnu9FbF0kRkzB80wF03Sw1JSwHnhd4B8DQITNjcisz-ojckTuGzVAU--n9NrjtFQw4-v0qpKqsZaRgmpBbuZ1v9COLrCXFQo7q500",
                                "alg": "RS256",
                                "dq": "Ccu_xKHLwGzfNwMq7gnqJnIuFCy8R72-1bpVLNq4JZZgc91iZbBcSVK7Ju3PuCiuAEvLsB1cHC91IF062cXkYhijZOalY_c2Ug2ERUtGr5X8eoDPUnZyccOefm37A0I5Aedra3n2AS8_FtqIwAMJVFC4bylUxkkBPoO0eHm24Yk",
                                "n": "plQx4or1C_Xany-wjM7mPHB4CAJPk3oOEdDSKpTwJ2dzGji5tEq7dUxExyhFN8f0PUjBjXyPph0gmDWaJG64fnhSSwVI-8Tdf2PppuK4rdCtWSPLgZ_DJ2DruxHgeXgwvJnX1HRfqhJF2p4ClkRUiVXZKFOhRPMGVgg18fnV9fXz5C4JacP_fmh498ktEohwcL3Pbv5DI_po_i0OiyF_M-9Iic3Ss80j22hs1wsNBGEMHvofWs7sl3ufwxmUCIstnDNSat840-n21Q4GV2v4L2kpROUw6l4ZmqZxoGl7eRSDS_VC5rPQoQEZYfyCiq6o1W5p9UXnoQin1zn0lr5Iaw"
                            }
                            """.trimIndent(),
                        "TZ" to "Europe/Oslo",
                        "JAVA_TOOL_OPTIONS" to "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
                    ),
                ).plus(
                    altinnTilgangerContainerHelper.envVars(),
                ),
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
