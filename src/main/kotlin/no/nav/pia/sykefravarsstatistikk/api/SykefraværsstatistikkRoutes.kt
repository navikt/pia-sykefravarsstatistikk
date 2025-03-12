package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.auth.OverordnetEnhetKey
import no.nav.pia.sykefravarsstatistikk.api.auth.TilgangerKey
import no.nav.pia.sykefravarsstatistikk.api.auth.UnderenhetKey
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.sykefraværsstatistikk(sykefraværsstatistikkService: SykefraværsstatistikkService) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    route("/sykefravarsstatistikk/{orgnr}") {
        route("/historikk/kvartalsvis") {
            get {
                logger.info("Henter kvartalsvis statistikk")
                val tilganger = call.attributes[TilgangerKey]
                val underenhet = call.attributes[UnderenhetKey]
                val overordnetEnhet = call.attributes[OverordnetEnhetKey]

                // TODO: Hvordan finne inneværende kvartal? Bare kall til db og sjekk høyeste årstall+kvartal?
                val årstall = 2024
                val kvartal = 4
                val sisteKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal)

                sykefraværsstatistikkService.hentKvartalsvisStatistikk(
                    sisteKvartal = sisteKvartal,
                    overordnetEnhet = overordnetEnhet,
                    underenhet = underenhet,
                    tilganger = tilganger,
                ).map {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = it, // TODO: tilDto her
                    )
                }.mapLeft {
                    call.respond(
                        status = it.httpStatusCode,
                        message = it.feilmelding,
                    )
                }
            }
        }

        route("/siste4kvartaler/aggregert") {
            get {
                logger.info("Henter aggregert statistikk siste fire kvartaler")
                val tilganger = call.attributes[TilgangerKey]
                val underenhet = call.attributes[UnderenhetKey]
                val årstall = 2024
                val kvartal = 4
                val inneværendeKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal)

                sykefraværsstatistikkService.hentAggregertStatistikk(
                    inneværendeKvartal = inneværendeKvartal,
                    underenhet = underenhet,
                    harEnkeltTilgang = tilganger.harEnkeltTilgang,
                ).map {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = it, // TODO: tilDto her
                    )
                }.mapLeft {
                    call.respond(
                        status = it.httpStatusCode,
                        message = it.feilmelding,
                    )
                }
            }
        }
    }
}
