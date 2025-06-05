package no.nav.pia.sykefravarsstatistikk.helper

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.log
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetINæringUtvinningAvRåoljeOgGass
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import software.xdev.mockserver.client.MockServerClient
import software.xdev.mockserver.model.Format
import software.xdev.mockserver.model.HttpRequest.request
import software.xdev.mockserver.model.HttpResponse.response
import software.xdev.testcontainers.mockserver.containers.MockServerContainer
import software.xdev.testcontainers.mockserver.containers.MockServerContainer.PORT

class AltinnTilgangerContainerHelper(
    network: Network = Network.newNetwork(),
    logger: Logger = LoggerFactory.getLogger(AltinnTilgangerContainerHelper::class.java),
) {
    private val networkAlias = "mockAltinnTilgangerContainer"
    private val port =
        PORT // mockserver default port er 1080 som MockServerContainer() eksponerer selv med "this.addExposedPort(1080);"
    private var mockServerClient: MockServerClient? = null

    val dockerImageName = DockerImageName.parse("xdevsoftware/mockserver:1.0.14")
    val altinnTilgangerContainer =
        MockServerContainer(dockerImageName)
            .withNetwork(network)
            .withNetworkAliases(networkAlias)
            .withLogConsumer(Slf4jLogConsumer(logger).withPrefix(networkAlias).withSeparateOutputStreams())
            .withEnv(
                mapOf(
                    "MOCKSERVER_LIVENESS_HTTP_GET_PATH" to "/isRunning",
                    "SERVER_PORT" to "$port",
                    "TZ" to "Europe/Oslo",
                ),
            )
            .waitingFor(Wait.forHttp("/isRunning").forStatusCode(200)).apply {
                start()
            }.also {
                logger.info("Startet (mock) altinnTilganger container for network '${network.id}' og port '$port'")
            }

    fun envVars() =
        mapOf(
            "ALTINN_TILGANGER_PROXY_URL" to "http://$networkAlias:$port",
        )

    private fun getMockServerClient(): MockServerClient {
        if (mockServerClient == null) {
            log.info(
                "Oppretter MockServerClient med host '${altinnTilgangerContainer.host}' og port '${
                    altinnTilgangerContainer.getMappedPort(port)
                }'",
            )
            mockServerClient = MockServerClient(
                altinnTilgangerContainer.host,
                altinnTilgangerContainer.getMappedPort(port),
            )
        }
        return mockServerClient!!
    }

    internal fun slettAlleRettigheter() {
        val client = getMockServerClient()

        runBlocking {
            val activeExpectations: String = client.retrieveActiveExpectations(
                request().withPath("/altinn-tilganger").withMethod("POST"),
                Format.JSON,
            )
            val expectations: List<Expectation> = Json.decodeFromString(activeExpectations)
            expectations.forEach { expectation ->
                client.clear(expectation.id)
            }
            log.info("Funnet og slettet '${expectations.size}' aktive expectations")
        }
    }

    internal fun leggTilRettigheter(
        overordnetEnhet: OverordnetEnhet = overordnetEnhetINæringUtvinningAvRåoljeOgGass.somOverordnetEnhet(),
        underenhet: Underenhet,
        altinn3Rettighet: String = "nav-ia-sykefravarsstatistikk-IKKE-SATT-OPP-ENDA",
    ) {
        log.info("Legger til rettigheter [altinn3: '$altinn3Rettighet'] for underenhet '${underenhet.orgnr}'")
        val client = getMockServerClient()
        runBlocking {
            client.`when`(
                request()
                    .withMethod("POST")
                    .withPath("/altinn-tilganger"),
            ).respond(
                response().withBody(
                    """
                    {
                      "hierarki": [
                        {
                          "orgnr": "${overordnetEnhet.orgnr}",
                          "altinn3Tilganger": [],
                          "altinn2Tilganger": [],
                          "underenheter": [
                            {
                              "orgnr": "${underenhet.orgnr}",
                              "altinn3Tilganger": [
                                "$altinn3Rettighet"
                              ],
                              "altinn2Tilganger": [],
                              "underenheter": [],
                              "navn": "${underenhet.navn}",
                              "organisasjonsform": "BEDR"
                            }
                          ],
                          "navn": "${overordnetEnhet.navn}",
                          "organisasjonsform": "ORGL"
                        }
                      ],
                      "orgNrTilTilganger": {
                        "${underenhet.orgnr}": [
                          "$altinn3Rettighet"
                        ]
                      },
                      "tilgangTilOrgNr": {
                        "$altinn3Rettighet": [
                          "${underenhet.orgnr}"
                        ]
                      },
                      "isError": false
                    }
                    """.trimIndent(),
                ),
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @JsonIgnoreUnknownKeys
    internal data class Expectation(
        val id: String,
    )
}
