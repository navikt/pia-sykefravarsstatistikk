package no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import no.nav.pia.sykefravarsstatistikk.http.helse

fun Application.configureRouting() {
    routing {
        helse()
    }
}
