package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.auth.TilgangerKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.organisasjoner() {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    route("/sykefravarsstatistikk/{orgnr}/organisasjoner") {
        route("/tilgang") {
            get {
                logger.info("Henter organisasjoner")
                val tilganger = call.attributes[TilgangerKey]

                call.respond(
                    status = HttpStatusCode.OK,
                    message = tilganger.altinnOrganisasjonerVedkommendeHarTilgangTil,
                )
            }
        }

        route("/enkeltrettighet") {
            get {
                logger.info("Henter organisasjoner med enkeltrettighet")
                val tilganger = call.attributes[TilgangerKey]

                call.respond(
                    status = HttpStatusCode.OK,
                    message = tilganger.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil,
                )
            }
        }
    }
}
