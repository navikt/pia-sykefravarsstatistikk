package no.nav.pia.sykefravarsstatistikk.helper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enOverordnetEnhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enOverordnetEnhetUtenStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetUtenStatistikk
import org.testcontainers.Testcontainers
import wiremock.com.google.common.net.HttpHeaders.CONTENT_TYPE

class AltinnMockHelper {
    companion object {
        private const val SERVICE_CODE = "3403"
        private const val SERVICE_EDITION = "2"
    }

    fun envVars() =
        mapOf(
            "ALTINN_RETTIGHETER_PROXY_CLIENT_ID" to "hei",
            "ALTINN_RETTIGHET_SERVICE_CODE" to SERVICE_CODE,
            "ALTINN_RETTIGHET_SERVICE_EDITION" to SERVICE_EDITION,
            "ALTINN_RETTIGHETER_PROXY_URL" to "http://host.testcontainers.internal:${altinnMock.port()}/altinn",
        )

    val altinnMock = WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
        it.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/altinn/v2/organisasjoner"))
                .willReturn(
                    WireMock.ok()
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(
                            """[
                            {
                                "Name": "${enUnderenhetIAltinn.navn}",
                                 "Type": "Business",
                                 "OrganizationNumber": "${enUnderenhetIAltinn.orgnr}",
                                 "ParentOrganizationNumber": "${enOverordnetEnhetIAltinn.orgnr}",
                                 "OrganizationForm": "BEDR",
                                 "Status": "Active"
                            }, 
                            {
                                "Name": "${enUnderenhetUtenStatistikk.navn}",
                                 "Type": "Business",
                                 "OrganizationNumber": "${enUnderenhetUtenStatistikk.orgnr}",
                                 "ParentOrganizationNumber": "${enOverordnetEnhetUtenStatistikk.orgnr}",
                                 "OrganizationForm": "BEDR",
                                 "Status": "Active"
                            }, 
                            {
                                "Name": "FIKTIVIA",
                                 "Type": "Business",
                                 "OrganizationNumber": "315829062",
                                 "ParentOrganizationNumber": "${enOverordnetEnhetIAltinn.orgnr}",
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
                .withQueryParam("serviceCode", equalTo(SERVICE_CODE)) // kan ikke hentes fra miljø først?
                .withQueryParam("serviceEdition", equalTo(SERVICE_EDITION))
                .willReturn(
                    WireMock.ok()
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody(
                            """[
                            {
                                "Name": "${enUnderenhetIAltinn.navn}",
                                 "Type": "Business",
                                 "OrganizationNumber": "${enUnderenhetIAltinn.orgnr}",
                                 "ParentOrganizationNumber": "${enOverordnetEnhetIAltinn.orgnr}",
                                 "OrganizationForm": "BEDR",
                                 "Status": "Active"
                            }, 
                            {
                                "Name": "${enUnderenhetUtenStatistikk.navn}",
                                 "Type": "Business",
                                 "OrganizationNumber": "${enUnderenhetUtenStatistikk.orgnr}",
                                 "ParentOrganizationNumber": "${enOverordnetEnhetUtenStatistikk.orgnr}",
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

        println("Starter Wiremock for Altinn på port ${it.port()}")
        Testcontainers.exposeHostPorts(it.port())
    }
}
