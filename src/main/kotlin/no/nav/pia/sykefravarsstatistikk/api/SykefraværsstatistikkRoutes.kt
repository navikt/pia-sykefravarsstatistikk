package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.aggregering.AggregertStatistikkService
import no.nav.pia.sykefravarsstatistikk.api.auth.OverordnetEnhetKey
import no.nav.pia.sykefravarsstatistikk.api.auth.VerifiserteTilgangerKey
import no.nav.pia.sykefravarsstatistikk.api.auth.UnderenhetKey
import no.nav.pia.sykefravarsstatistikk.persistering.KvartalsvisSykefraværshistorikkService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Route.sykefraværsstatistikk(
    aggregertStatistikkService: AggregertStatistikkService,
    kvartalsvisSykefraværshistorikkService: KvartalsvisSykefraværshistorikkService,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
    route("/sykefravarsstatistikk/{orgnr}") {
        route("/historikk/kvartalsvis") {
            get {
                logger.info("Henter kvartalsvis statistikk")

                kvartalsvisSykefraværshistorikkService.hentSykefraværshistorikk(
                    overordnetEnhet = call.attributes[OverordnetEnhetKey],
                    underenhet = call.attributes[UnderenhetKey],
                    tilganger = call.attributes[VerifiserteTilgangerKey],
                ).map {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = it,
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

                aggregertStatistikkService.hentAggregertStatistikk(
                    underenhet = call.attributes[UnderenhetKey],
                    tilganger = call.attributes[VerifiserteTilgangerKey],
                ).map {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = it,
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
