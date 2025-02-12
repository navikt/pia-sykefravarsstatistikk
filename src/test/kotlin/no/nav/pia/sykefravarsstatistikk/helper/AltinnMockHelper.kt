package no.nav.pia.sykefravarsstatistikk.helper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.testcontainers.Testcontainers
import wiremock.com.google.common.net.HttpHeaders.CONTENT_TYPE

class AltinnMockHelper {

    companion object {
        private const val SERVICE_CODE = "5934"
        private const val SERVICE_EDITION = "1"

        val wireMock = WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
            it.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/altinn/v2/organisasjoner"))
                    .willReturn(
                        WireMock.ok()
                            .withHeader(CONTENT_TYPE, "application/json")
                            .withBody(
                                """[
                                {
                                    "Name": "BALLSTAD OG HAMARØY",
                                     "Type": "Business",
                                     "OrganizationNumber": "811076732",
                                     "ParentOrganizationNumber": "811076112",
                                     "OrganizationForm": "BEDR",
                                     "Status": "Active"
                                }, 
                                {
                                    "Name": "FIKTIVIA",
                                     "Type": "Business",
                                     "OrganizationNumber": "315829062",
                                     "ParentOrganizationNumber": "811076112",
                                     "OrganizationForm": "BEDR",
                                     "Status": "Active"
                                }
                            ]
                                """.trimMargin(),
                            ),
                    ),
            )
            it.stubFor(
                WireMock.get(WireMock.urlPathEqualTo("/altinn/v2/organisasjoner"))
                    .withQueryParam("serviceCode", equalTo(SERVICE_CODE))
                    .withQueryParam("serviceEdition", equalTo(SERVICE_EDITION))
                    .willReturn(
                        WireMock.ok()
                            .withHeader(CONTENT_TYPE, "application/json")
                            .withBody(
                                """[
                                {
                                    "Name": "BALLSTAD OG HAMARØY",
                                     "Type": "Business",
                                     "OrganizationNumber": "811076732",
                                     "ParentOrganizationNumber": "811076112",
                                     "OrganizationForm": "BEDR",
                                     "Status": "Active"
                                }
                            ]
                                """.trimMargin(),
                            ),
                    ),
            )

            if (!it.isRunning) {
                it.start()
            }

            println("Starter Wiremock på port ${it.port()}")
            Testcontainers.exposeHostPorts(it.port())
        }

    }
}
