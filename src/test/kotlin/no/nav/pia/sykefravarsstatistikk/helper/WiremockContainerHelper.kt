package no.nav.pia.sykefravarsstatistikk.helper

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.testcontainers.Testcontainers

class WiremockContainerHelper {
    private val brregMock: WireMockServer

    init {
        brregMock = lagMockServer()
    }

    private fun lagMockServer() =
        WireMockServer(WireMockConfiguration.options().dynamicPort()).also {
            if (!it.isRunning) {
                it.start()
            }

            println("Starter Wiremock for 'brreg' på port ${it.port()}")
            Testcontainers.exposeHostPorts(it.port())
        }

    fun envVars() =
        mapOf(
            "ENHETSREGISTERET_URL" to "http://host.testcontainers.internal:${brregMock.port()}",
        )
}
