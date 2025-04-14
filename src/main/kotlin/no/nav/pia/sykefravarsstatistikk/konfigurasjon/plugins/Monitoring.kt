package no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.pia.sykefravarsstatistikk.Metrics

fun Application.configureMonitoring() {
    install(MicrometerMetrics) {
        registry = Metrics.appMicrometerRegistry
    }
    routing {
        get("/metrics") {
            call.respond(Metrics.appMicrometerRegistry.scrape())
        }
    }
}
