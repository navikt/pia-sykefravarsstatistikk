package no.nav.pia.sykefravarsstatistikk.helper

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregEnhetDto
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregUnderenhetDto
import no.nav.pia.sykefravarsstatistikk.helper.AltinnTilgangerContainerHelper.Expectation
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.log
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.virksomheterMedBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.virksomheterMedEdgeCase
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.virksomheterMedNæring
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
import software.xdev.mockserver.model.MediaType
import software.xdev.mockserver.model.RequestDefinition
import software.xdev.testcontainers.mockserver.containers.MockServerContainer
import software.xdev.testcontainers.mockserver.containers.MockServerContainer.PORT

class EnhetsregisteretContainerHelper(
    network: Network = Network.newNetwork(),
    logger: Logger = LoggerFactory.getLogger(EnhetsregisteretContainerHelper::class.java),
) {
    private val networkAlias = "mockEnhetsregisteretContainer"
    private val port =
        PORT // mockserver default port er 1080 som MockServerContainer() eksponerer selv med "this.addExposedPort(1080);"
    private var mockServerClient: MockServerClient? = null

    val dockerImageName = DockerImageName.parse("xdevsoftware/mockserver:1.0.14")
    val enhetsregisteretContainer = MockServerContainer(dockerImageName)
        .withNetwork(network)
        .withNetworkAliases(networkAlias)
        // OBS: logging starter på level WARN i logback-test.xml (mockserver er veldig verbose),
        // -> skru ned til INFO ved feilsøking i testene
        .withLogConsumer(Slf4jLogConsumer(logger).withPrefix(networkAlias).withSeparateOutputStreams())
        .withEnv(
            mapOf(
                "MOCKSERVER_LIVENESS_HTTP_GET_PATH" to "/isRunning",
                "SERVER_PORT" to "$port",
                "TZ" to "Europe/Oslo",
            ),
        )
        .waitingFor(Wait.forHttp("/isRunning").forStatusCode(200))
        .apply {
            start()
        }.also {
            logger.info("Startet (mock) enhetsregisteret container for network '${network.id}' og port '$port'")
        }

    fun envVars() =
        mapOf(
            "ENHETSREGISTERET_URL" to "http://$networkAlias:$port",
        )

    fun opprettAlleTestvirksomheterIEnhetsregisteret() {
        slettAlleEnheterOgUnderenheter()
        virksomheterMedBransje.forEach {
            leggTilIEnhetsregisteret(it.first, it.second)
        }
        log.info("Opprettet ${virksomheterMedBransje.size} virksomheter (med bransje) i enhetsregisteret")
        virksomheterMedNæring.forEach {
            leggTilIEnhetsregisteret(it.first, it.second)
        }
        log.info("Opprettet ${virksomheterMedNæring.size} virksomheter (med næring) i enhetsregisteret")
        virksomheterMedEdgeCase.forEach {
            leggTilIEnhetsregisteret(it.first, it.second)
        }
        log.info("Opprettet ${virksomheterMedEdgeCase.size} virksomheter (med 'edge case') i enhetsregisteret")
        val antallExpectationsOpprettet = hentAlleExpectationIds()
        log.info("Har nå $antallExpectationsOpprettet testvirksomheter i enhetsregisteret")
    }


    private fun getMockServerClient(): MockServerClient {
        if (mockServerClient == null) {
            log.info(
                "Oppretter MockServerClient i enhetsregisteretContainer med host '${enhetsregisteretContainer.host}' og port '${
                    enhetsregisteretContainer.getMappedPort(port)
                }'",
            )
            mockServerClient = MockServerClient(
                enhetsregisteretContainer.host,
                enhetsregisteretContainer.getMappedPort(port),
            )
        }
        return mockServerClient!!
    }

    // private funksjon så det skal ikke være mulig å slette alle virksomheter i enhetsregisteret,
    // da de kan være tatt i bruk av flere tester (lag en slett(expectationId) funksjon i stedet)
    private fun slettAlleEnheterOgUnderenheter() {
        val client = getMockServerClient()
        val allExpectations = hentAlleExpectationIds()

        runBlocking {
            allExpectations.forEach { expectation ->
                client.clear(expectation.id)
            }
            log.info("Funnet og slettet '${allExpectations.size}' aktive expectations")
        }
    }

    internal fun hentAlleExpectationIds(): List<Expectation> {
        val client = getMockServerClient()
        return runBlocking {
            val alleAktiveRequestDefinition: RequestDefinition? = null
            val activeExpectations = client.retrieveActiveExpectations(alleAktiveRequestDefinition, Format.JSON)
            Json.decodeFromString<List<Expectation>>(activeExpectations)
        }
    }

    internal fun leggTilIEnhetsregisteret(
        overordnetEnhet: BrregEnhetDto,
        underenhet: BrregUnderenhetDto,
    ) {
        leggTilOverordnetEnhet(overordnetEnhet = overordnetEnhet)
        leggTilUnderenhet(underenhet = underenhet)
    }

    private fun leggTilUnderenhet(
        underenhet: BrregUnderenhetDto,
    ) {
        log.debug(
            "Legger til underenhet med orgnr '${underenhet.organisasjonsnummer}' og navn '${underenhet.navn}'",
        )
        val næringskode = if (underenhet.naeringskode1 != null) {
            """
              "naeringskode1": {
                "kode": "${underenhet.naeringskode1!!.kode}",
                "beskrivelse": "${underenhet.naeringskode1!!.beskrivelse}"
              },
            """.trimIndent()
        } else {
            ""
        }

        val client = getMockServerClient()
        runBlocking {
            client.`when`(
                request()
                    .withMethod("GET")
                    .withPath("/enhetsregisteret/api/underenheter/${underenhet.organisasjonsnummer}"),
            ).respond(
                response().withBody(
                    """
                    {
                      "organisasjonsnummer": "${underenhet.organisasjonsnummer}",
                      "navn": "${underenhet.navn}",
                      "organisasjonsform": {
                        "kode": "AS",
                        "beskrivelse": "Aksjeselskap",
                        "_links": {
                          "self": {
                            "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/AS"
                          }
                        }
                      },
                      "postadresse": {
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "0661",
                        "poststed": "OSLO",
                        "adresse": [
                          "Default adresse"
                        ],
                        "kommune": "OSLO",
                        "kommunenummer": "0301"
                      },
                      "registreringsdatoEnhetsregisteret": "1992-09-18",
                      "registrertIMvaregisteret": true,
                      ${næringskode}
                      "antallAnsatte": ${underenhet.antallAnsatte},
                      "harRegistrertAntallAnsatte": true,
                      "overordnetEnhet": "${underenhet.overordnetEnhet}",
                      "registreringsdatoAntallAnsatteEnhetsregisteret": "1992-09-18",
                      "registreringsdatoAntallAnsatteNAVAaregisteret": "1992-09-18",
                        "epostadresse": "epost@epost.no",
                        "mobil": "10 20 30 40",
                        "oppstartsdato": "1992-09-18",
                        "beliggenhetsadresse": {
                          "land": "Norge",
                          "landkode": "NO",
                          "postnummer": "",
                          "poststed": "",
                          "adresse": [
                            ""
                          ],
                          "kommune": "BERGEN",
                          "kommunenummer": "4601"
                        },
                      "_links": {
                        "self": {
                          "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/${underenhet.organisasjonsnummer}"
                        },
                        "overordnetEnhet": {
                          "href": "https://data.brreg.no/enhetsregisteret/api/enheter/${underenhet.overordnetEnhet}"
                        }
                      }
                    }
                    """.trimIndent(),
                ).withContentType(MediaType.APPLICATION_JSON_UTF_8),
            )
        }
    }

    private fun leggTilOverordnetEnhet(
        overordnetEnhet: BrregEnhetDto,
    ) {
        log.debug(
            "Legger til overordnet enhet med orgnr '${overordnetEnhet.organisasjonsnummer}' og navn '${overordnetEnhet.navn}'",
        )
        val institusjonellSektorkode = if (overordnetEnhet.institusjonellSektorkode != null) {
            """
            "institusjonellSektorkode": {
                "kode": "${overordnetEnhet.institusjonellSektorkode!!.kode}",
                "beskrivelse": "${overordnetEnhet.institusjonellSektorkode!!.beskrivelse}"
            },
            """.trimIndent()
        } else {
            ""
        }

        val client = getMockServerClient()
        runBlocking {
            client.`when`(
                request()
                    .withMethod("GET")
                    .withPath("/enhetsregisteret/api/enheter/${overordnetEnhet.organisasjonsnummer}"),
            ).respond(
                response().withBody(
                    """
                    {
                      "organisasjonsnummer": "${overordnetEnhet.organisasjonsnummer}",
                      "navn": "${overordnetEnhet.navn}",
                      "organisasjonsform": {
                        "kode": "AS",
                        "beskrivelse": "Aksjeselskap",
                        "_links": {
                          "self": {
                            "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/AS"
                          }
                        }
                      },
                      "registreringsdatoEnhetsregisteret": "1992-09-18",
                      "registrertIMvaregisteret": true,
                      "naeringskode1": {
                        "kode": "${overordnetEnhet.naeringskode1.kode}",
                        "beskrivelse": "${overordnetEnhet.naeringskode1.beskrivelse}"
                      },
                      "antallAnsatte": ${overordnetEnhet.antallAnsatte},
                      "harRegistrertAntallAnsatte": true,
                      "registreringsdatoMerverdiavgiftsregisteret": "1992-09-18",
                      "registreringsdatoMerverdiavgiftsregisteretEnhetsregisteret": "1992-09-18",
                      "registreringsdatoAntallAnsatteEnhetsregisteret": "1992-09-18",
                      "registreringsdatoAntallAnsatteNAVAaregisteret": "1992-09-18",
                      "epostadresse": "epost@epost.no",
                      "mobil": "10 20 30 40",
                      "forretningsadresse": {
                        "land": "Norge",
                        "landkode": "NO",
                        "postnummer": "",
                        "poststed": "",
                        "adresse": [
                          ""
                        ],
                        "kommune": "SVALBARD",
                        "kommunenummer": "2100"
                      },
                      "stiftelsesdato": "1992-09-18",
                      ${institusjonellSektorkode}
                      "registrertIForetaksregisteret": true,
                      "registrertIStiftelsesregisteret": false,
                      "registrertIFrivillighetsregisteret": false,
                      "sisteInnsendteAarsregnskap": "1992",
                      "konkurs": false,
                      "underAvvikling": false,
                      "underTvangsavviklingEllerTvangsopplosning": false,
                      "maalform": "Bokmål",
                      "vedtektsdato": "1992-09-18",
                      "vedtektsfestetFormaal": [
                        ""
                      ],
                      "aktivitet": [
                        ""
                      ],
                      "registreringsdatoForetaksregisteret": "1992-09-18",
                      "registrertIPartiregisteret": false,
                      "_links": {
                        "self": {
                          "href": "https://data.brreg.no/enhetsregisteret/api/enheter/${overordnetEnhet.organisasjonsnummer}",
                        }
                      }
                    }
                    """.trimIndent(),
                ).withContentType(MediaType.APPLICATION_JSON_UTF_8),
            )
        }
    }
}
