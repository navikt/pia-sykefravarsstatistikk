package no.nav.pia.sykefravarsstatistikk.helper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.testcontainers.Testcontainers

class WiremockContainerHelper {
    private val altinnMock: WireMockServer
    private val brregMock: WireMockServer

    companion object {
        private const val SERVICE_CODE = "3403"
        private const val SERVICE_EDITION = "2"
    }

    init {
        altinnMock = lagMockServer("altinn")
        brregMock = lagMockServer("brreg")
    }

    private fun lagMockServer(service: String) =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
            if (!it.isRunning) {
                it.start()
            }

            println("Starter Wiremock for $service på port ${it.port()}")
            Testcontainers.exposeHostPorts(it.port())
        }

    fun envVars() =
        mapOf(
            "BRREG_URL" to "http://host.testcontainers.internal:${brregMock.port()}",
            "ALTINN_RETTIGHETER_PROXY_CLIENT_ID" to "hei",
            "ALTINN_RETTIGHET_SERVICE_CODE" to SERVICE_CODE,
            "ALTINN_RETTIGHET_SERVICE_EDITION" to SERVICE_EDITION,
            "ALTINN_RETTIGHETER_PROXY_URL" to "http://host.testcontainers.internal:${altinnMock.port()}/altinn",
        )
}
