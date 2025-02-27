package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.auth.OverordnetEnhetKey
import no.nav.pia.sykefravarsstatistikk.api.auth.TilgangerKey
import no.nav.pia.sykefravarsstatistikk.api.auth.UnderenhetKey
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilDto
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.http.orgnr
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService

fun Route.sykefraværsstatistikk(sykefraværsstatistikkService: SykefraværsstatistikkService) {
    route("/{orgnr}/sykefravarshistorikk") {
        route("/kvartalsvis") {
            get {
                val tilganger = call.attributes[TilgangerKey]
                val underenhet = call.attributes[UnderenhetKey]
                val overordnetEnhet = call.attributes[OverordnetEnhetKey]

                // TODO: Hvordan finne sektor
                val sektor = "1"
                // TODO: Hvordan finne inneværende kvartal? Bare kall til db og sjekk høyeste årstall+kvartal?
                val årstall = 2024
                val kvartal = 4
                val sisteKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal)

                // Kvartal 5 år tilbake
                val førsteKvartal = sisteKvartal.minusKvartaler(20)

                val response: MutableList<KvartalsvisSykefraværshistorikkDto> = mutableListOf()

                underenhet.bransje()?.let { bransje ->
                    val bransjestatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkBransje(
                        bransje = bransje.name,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(message = "Ingen statistikk funnet or bransje '${bransje.name}'", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    response.add(bransjestatistikk.tilDto(type = BRANSJE.name, label = bransje.name))
                } ?: call.application.log.info("${underenhet.orgnr} er ikke en del av en bransje")

                response.add(
                    if (tilganger.harEnkeltTilgangOverordnetEnhet) {
                        val overordnetEnhetStatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkVirksomhet(
                            orgnr = overordnetEnhet.orgnr,
                            førsteÅrstalOgKvartal = førsteKvartal,
                        ).ifEmpty {
                            call.respond(
                                message = "Ingen virksomhetsstatistikk funnet for overordnet enhet '${overordnetEnhet.orgnr}'",
                                status = HttpStatusCode.BadRequest,
                            )
                            return@get
                        }
                        overordnetEnhetStatistikk.tilDto(type = "OVERORDNET_ENHET", label = overordnetEnhet.navn)
                    } else {
                        KvartalsvisSykefraværshistorikkDto(
                            type = "OVERORDNET_ENHET",
                            label = overordnetEnhet.navn,
                            kvartalsvisSykefraværsprosent = emptyList(),
                        )
                    },
                )

                if (tilganger.harEnkeltTilgang) {
                    val virksomhetsstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkVirksomhet(
                        orgnr = underenhet.orgnr,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(
                            message = "Ingen virksomhetsstatistikk funnet for underenhet '${underenhet.orgnr}'",
                            status = HttpStatusCode.BadRequest,
                        )
                        return@get
                    }

                    response.add(virksomhetsstatistikk.tilDto(type = VIRKSOMHET.name, label = underenhet.navn))
                }

                // TODO: Hvordan utlede sektor?
                val sektorstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkSektor(
                    sektor = sektor,
                    førsteÅrstalOgKvartal = førsteKvartal,
                ).ifEmpty {
                    call.respond(message = "Ingen sektorstatistikk funnet", status = HttpStatusCode.BadRequest)
                    return@get
                }
                response.add(sektorstatistikk.tilDto(type = SEKTOR.name, label = sektor))

                val landstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkLand(
                    førsteKvartal,
                ).ifEmpty {
                    call.respond(message = "Ingen landstatistikk funnet", status = HttpStatusCode.BadRequest)
                    return@get
                }
                response.add(landstatistikk.tilDto(type = "LAND", label = "Norge"))

                call.respond(
                    status = HttpStatusCode.OK,
                    message = response,
                )
            }
        }
    }
}
