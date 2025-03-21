package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.persistering.PubliseringsdatoService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.publiseringsdato(publiseringsdatoService: PubliseringsdatoService) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    route("/sykefravarsstatistikk/publiseringsdato") {
        get {
            logger.info("Henter publiseringsdatoer")
            val publiseringsdatoer = publiseringsdatoService.hentPubliseringsdatoer()
            val publiseringskalender = publiseringsdatoService.hentPubliseringskalender(publiseringsdatoer)
            call.respond(
                status = HttpStatusCode.OK,
                message = publiseringskalender,
            )
        }
    }
}
