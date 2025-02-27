package no.nav.pia.sykefravarsstatistikk.helper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enOverordnetEnhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enVirksomhetUtenTilgangIAltinn
import org.testcontainers.Testcontainers
import wiremock.com.google.common.net.HttpHeaders.CONTENT_TYPE

class BrregMockHelper {
    fun envVars() =
        mapOf(
            "BRREG_URL" to "http://host.testcontainers.internal:${brregMock.port()}",
        )

    companion object {
        private val overordnetEnhetJson = """{
  "organisasjonsnummer": "${enOverordnetEnhetIAltinn.orgnr}",
  "navn": "${enOverordnetEnhetIAltinn.navn}",
  "organisasjonsform": {
    "kode": "ORGL",
    "beskrivelse": "Organisasjonsledd",
    "_links": {
      "self": {
        "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/ORGL"
      }
    }
  },
  "hjemmeside": "www.nav.no",
  "postadresse": {
    "land": "Norge",
    "landkode": "NO",
    "postnummer": "8601",
    "poststed": "MO I RANA",
    "adresse": [
      "Postboks 354"
    ],
    "kommune": "RANA",
    "kommunenummer": "1833"
  },
  "registreringsdatoEnhetsregisteret": "2006-03-23",
  "registrertIMvaregisteret": true,
  "naeringskode1": {
    "kode": "${enOverordnetEnhetIAltinn.næringskode.femsifferIdentifikator}",
    "beskrivelse": "Offentlig administrasjon tilknyttet helsestell, sosial virksomhet, undervisning, kirke, kultur og miljøvern"
  },
  "antallAnsatte": ${enOverordnetEnhetIAltinn.antallAnsatte},
  "harRegistrertAntallAnsatte": true,
  "overordnetEnhet": "983887457",
  "registreringsdatoMerverdiavgiftsregisteret": "2006-07-01",
  "registreringsdatoMerverdiavgiftsregisteretEnhetsregisteret": "2006-10-04",
  "registreringsdatoAntallAnsatteEnhetsregisteret": "2025-02-12",
  "registreringsdatoAntallAnsatteNAVAaregisteret": "2025-02-10",
  "telefon": "21 07 00 00",
  "forretningsadresse": {
    "land": "Norge",
    "landkode": "NO",
    "postnummer": "0661",
    "poststed": "OSLO",
    "adresse": [
      "Fyrstikkalléen 1"
    ],
    "kommune": "OSLO",
    "kommunenummer": "0301"
  },
  "institusjonellSektorkode": {
    "kode": "6100",
    "beskrivelse": "Statsforvaltningen"
  },
  "registrertIForetaksregisteret": false,
  "registrertIStiftelsesregisteret": false,
  "registrertIFrivillighetsregisteret": false,
  "konkurs": false,
  "underAvvikling": false,
  "underTvangsavviklingEllerTvangsopplosning": false,
  "maalform": "Bokmål",
  "aktivitet": [
    "Arbeids- og velferdsetaten har ansvaret for gjennomføringen av",
    "arbeidsmarkeds- trygde- og pensjonspolitikken"
  ],
  "registrertIPartiregisteret": false,
  "_links": {
    "self": {
      "href": "https://data.brreg.no/enhetsregisteret/api/enheter/889640782"
    },
    "overordnetEnhet": {
      "href": "https://data.brreg.no/enhetsregisteret/api/enheter/983887457"
    }
  }
}
        """.trimMargin()

        private val underEnhetUtenTilgang =
            """
    {
  "organisasjonsnummer": "${enVirksomhetUtenTilgangIAltinn.orgnr}",
  "navn": "${enVirksomhetUtenTilgangIAltinn.navn}",
  "organisasjonsform": {
    "kode": "BEDR",
    "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning",
    "_links": {
      "self": {
        "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/BEDR"
      }
    }
  },
  "registreringsdatoEnhetsregisteret": "2023-06-12",
  "registrertIMvaregisteret": false,
  "naeringskode1": {
    "kode": "${enVirksomhetUtenTilgangIAltinn.næringskode.femsifferIdentifikator}",
    "beskrivelse": "Offentlig administrasjon tilknyttet helsestell, sosial virksomhet, undervisning, kirke, kultur og miljøvern"
  },
  "harRegistrertAntallAnsatte": true,
  "overordnetEnhet": "${enOverordnetEnhetIAltinn.orgnr}",
  "registreringsdatoAntallAnsatteEnhetsregisteret": "2024-02-12",
  "registreringsdatoAntallAnsatteNAVAaregisteret": "2024-02-10",
  "oppstartsdato": "2023-06-01",
  "beliggenhetsadresse": {
    "land": "Norge",
    "landkode": "NO",
    "postnummer": "5009",
    "poststed": "BERGEN",
    "adresse": [
      "Møllendalsveien 8"
    ],
    "kommune": "BERGEN",
    "kommunenummer": "4601"
  },
  "_links": {
    "self": {
      "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/931561847"
    },
    "overordnetEnhet": {
      "href": "https://data.brreg.no/enhetsregisteret/api/enheter/889640782"
    }
  }
}
            """.trimIndent()

        private val underenhetJson = """{
  "organisasjonsnummer": "${enUnderenhetIAltinn.orgnr}",
  "navn": "${enUnderenhetIAltinn.navn}",
  "organisasjonsform": {
    "kode": "BEDR",
    "beskrivelse": "Underenhet til næringsdrivende og offentlig forvaltning",
    "_links": {
      "self": {
        "href": "https://data.brreg.no/enhetsregisteret/api/organisasjonsformer/BEDR"
      }
    }
  },
  "postadresse": {
    "land": "Norge",
    "landkode": "NO",
    "postnummer": "8601",
    "poststed": "MO I RANA",
    "adresse": [
      "Postboks 354"
    ],
    "kommune": "RANA",
    "kommunenummer": "1833"
  },
  "registreringsdatoEnhetsregisteret": "2013-12-23",
  "registrertIMvaregisteret": false,
  "naeringskode1": {
    "kode": "${enUnderenhetIAltinn.næringskode.femsifferIdentifikator}",
    "beskrivelse": "Offentlig administrasjon tilknyttet helsestell, sosial virksomhet, undervisning, kirke, kultur og miljøvern"
  },
  "antallAnsatte": ${enUnderenhetIAltinn.antallAnsatte},
  "harRegistrertAntallAnsatte": true,
  "overordnetEnhet": "${enOverordnetEnhetIAltinn.orgnr}",
  "registreringsdatoAntallAnsatteEnhetsregisteret": "2025-02-11",
  "registreringsdatoAntallAnsatteNAVAaregisteret": "2025-02-10",
  "oppstartsdato": "2013-12-01",
  "beliggenhetsadresse": {
    "land": "Norge",
    "landkode": "NO",
    "postnummer": "0661",
    "poststed": "OSLO",
    "adresse": [
      "Fyrstikkalléen 1"
    ],
    "kommune": "OSLO",
    "kommunenummer": "0301"
  },
  "_links": {
    "self": {
      "href": "https://data.brreg.no/enhetsregisteret/api/underenheter/912998827"
    },
    "overordnetEnhet": {
      "href": "https://data.brreg.no/enhetsregisteret/api/enheter/889640782"
    }
  }
}
        """.trimMargin()

        val brregMock = WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
            it.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/enhetsregisteret/api/enheter/${enOverordnetEnhetIAltinn.orgnr}"))
                    .willReturn(
                        WireMock.ok()
                            .withHeader(CONTENT_TYPE, "application/json")
                            .withBody(overordnetEnhetJson),
                    ),
            )
            it.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/enhetsregisteret/api/underenheter/${enUnderenhetIAltinn.orgnr}"))
                    .willReturn(
                        WireMock.ok()
                            .withHeader(CONTENT_TYPE, "application/json")
                            .withBody(underenhetJson),
                    ),
            )
            it.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/enhetsregisteret/api/underenheter/${enVirksomhetUtenTilgangIAltinn.orgnr}"))
                    .willReturn(
                        WireMock.ok()
                            .withHeader(CONTENT_TYPE, "application/json")
                            .withBody(underEnhetUtenTilgang),
                    ),
            )

            if (!it.isRunning) {
                it.start()
            }

            println("Starter Wiremock for Brreg på port ${it.port()}")
            Testcontainers.exposeHostPorts(it.port())
        }
    }
}
