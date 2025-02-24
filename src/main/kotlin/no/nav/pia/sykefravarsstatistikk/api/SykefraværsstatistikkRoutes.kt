package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilBransjeDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilLandDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilSektorDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilVirksomhetDto
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.http.tokenSubject
import no.nav.pia.sykefravarsstatistikk.http.virksomhet
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService

fun Route.sykefraværsstatistikk(sykefraværsstatistikkService: SykefraværsstatistikkService) {
    route("/{orgnr}/sykefravarshistorikk/kvartalsvis") {
        get("/") {
            val fnr = call.request.tokenSubject()
            // TODO: sjekk om fnr har tilgang til orgnr
            // TODO: Har bruker alle rettighetene til å se sykefraværsstatistikk for denne virksomheten?

            val orgnr = call.virksomhet.orgnr
            val årstall = 2024
            val kvartal = 4
            val inneværendeKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal)

            val førsteKvartal = inneværendeKvartal.minusKvartaler(20) // 5 år = 20 kvartaler?

            // TODO: Lag underenhetservice og hent underenhet
            // TODO: Bruk wiremock til å hente underenhet
            //  - Bør få overordnet enhets orgnr?
            //  - Hent overordnet enhet?
            //  - Sjekk om bruker har tilgang til overordnet enhet?
            //  - ikke næringsdrivende ?

            val overordnetEnhetStatistikk = sykefraværsstatistikkService.hentKvartalsvisOverordnetEnhet(
                orgnr = orgnr,
                førsteÅrstalOgKvartal = førsteKvartal,
            )
            val virksomhetsstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkVirksomhet(
                orgnr = orgnr,
                førsteÅrstalOgKvartal = førsteKvartal,
            )

            val bransje = "Sykehjem"
            val sektor = "1"

            val bransjestatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkBransje(
                bransje = bransje,
                førsteÅrstalOgKvartal = førsteKvartal,
            )
            val sektorstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkSektor(
                sektor = sektor,
                førsteÅrstalOgKvartal = førsteKvartal,
            )
            val landstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkLand(førsteKvartal)

            if (virksomhetsstatistikk.isEmpty()) {
                call.respond(message = "Ingen virksomhetsstatistikk funnet", status = HttpStatusCode.BadRequest)
                return@get
            }

            if (bransjestatistikk.isEmpty()) {
                call.respond(message = "Ingen bransjestatistikk funnet", status = HttpStatusCode.BadRequest)
                return@get
            }

            if (sektorstatistikk.isEmpty()) {
                call.respond(message = "Ingen sektorstatistikk funnet", status = HttpStatusCode.BadRequest)
                return@get
            }

            if (landstatistikk.isEmpty()) {
                call.respond(message = "Ingen landstatistikk funnet", status = HttpStatusCode.BadRequest)
                return@get
            }

            val aggregertStatistikk: List<KvartalsvisSykefraværshistorikkDto> =
                listOf(
                    overordnetEnhetStatistikk,
                    virksomhetsstatistikk.tilVirksomhetDto(),
                    bransjestatistikk.tilBransjeDto(bransje = bransje),
                    sektorstatistikk.tilSektorDto(sektor = sektor),
                    landstatistikk.tilLandDto(),
                )

            call.respond(
                status = HttpStatusCode.OK,
                message = aggregertStatistikk,
            )
        }
    }
}
