package no.nav.pia.sykefravarsstatistikk.helper

import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.log
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetUtenTilgang
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

class AltinnTilgangerContainerHelper(
    network: Network = Network.newNetwork(),
    logger: Logger = LoggerFactory.getLogger(AltinnTilgangerContainerHelper::class.java),
) {
    private val networkAlias = "mockAltinnTilgangerContainer"
    private val port = 7070

    val dockerImageName = DockerImageName.parse("mockserver/mockserver")
    val altinnTilgangerContainer = GenericContainer(dockerImageName)
        .withNetwork(network)
        .withNetworkAliases(networkAlias)
        .withExposedPorts(port)
        .withLogConsumer(Slf4jLogConsumer(logger).withPrefix(networkAlias).withSeparateOutputStreams())
        .withEnv(
            mapOf(
                "MOCKSERVER_LIVENESS_HTTP_GET_PATH" to "/isRunning",
                "SERVER_PORT" to "7070",
                "TZ" to "Europe/Oslo",
            ),
        )
        .waitingFor(Wait.forHttp("/isRunning").forStatusCode(200))
        .apply {
            start()
        }.also {
            logger.info("Startet (mock) altinnTilganger container for network '$network' og port '$port'")
        }

    fun envVars() =
        mapOf(
            "ALTINN_TILGANGER_PROXY_URL" to "http://$networkAlias:$port",
        )

    internal fun slettAlleRettigheter() {
        val client = MockServerClient(
            altinnTilgangerContainer.host,
            altinnTilgangerContainer.getMappedPort(7070),
        )
        client.reset()
    }

    internal fun leggTilRettigheter(
        overordnetEnhet: OverordnetEnhet = overordnetEnhetUtenTilgang,
        underenhet: Underenhet,
        altinn2Rettighet: String = "",
        altinn3Rettighet: String = "nav-ia-sykefravarsstatistikk-IKKE-SATT-OPP-ENDA",
    ) {
        log.debug(
            "Oppretter MockServerClient med host '${altinnTilgangerContainer.host}' og port '${
                altinnTilgangerContainer.getMappedPort(
                    7070,
                )
            }'. Og legger til rettighet '$altinn2Rettighet' for underenhet '${underenhet.orgnr}'",
        )
        val client = MockServerClient(
            altinnTilgangerContainer.host,
            altinnTilgangerContainer.getMappedPort(7070),
        )
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
                              "altinn2Tilganger": [
                                "$altinn2Rettighet"
                              ],
                              "underenheter": [],
                              "navn": "${(underenhet as Underenhet.Næringsdrivende).navn}",
                              "organisasjonsform": "BEDR"
                            }
                          ],
                          "navn": "${overordnetEnhet.navn}",
                          "organisasjonsform": "ORGL"
                        }
                      ],
                      "orgNrTilTilganger": {
                        "${underenhet.orgnr}": [
                          "$altinn3Rettighet",
                          "$altinn2Rettighet"
                        ]
                      },
                      "tilgangTilOrgNr": {
                        "$altinn3Rettighet": [
                          "${underenhet.orgnr}"
                        ],
                        "$altinn2Rettighet": [
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
}
