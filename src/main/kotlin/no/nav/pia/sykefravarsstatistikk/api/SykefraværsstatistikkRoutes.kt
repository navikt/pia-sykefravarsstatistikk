package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.tilDto
import no.nav.pia.sykefravarsstatistikk.http.tokenSubject
import no.nav.pia.sykefravarsstatistikk.http.virksomhet
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService

fun Route.sykefraværsstatistikk(sykefraværsstatistikkService: SykefraværsstatistikkService) {
    route("/{orgnr}/sykefravarshistorikk/kvartalsvis") {
        get("/") {
            val fnr = call.request.tokenSubject()
            val orgnr = call.virksomhet.orgnr
            call.respond(
                sykefraværsstatistikkService.hentSykefraværsstatistikkVirksomhet(fnr, orgnr).map { it.tilDto() },
            )
        }
    }
}
