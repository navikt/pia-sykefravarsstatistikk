package no.nav.pia.sykefravarsstatistikk.api

import ia.felles.definisjoner.bransjer.Bransje
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.auth.OverordnetEnhetKey
import no.nav.pia.sykefravarsstatistikk.api.auth.TilgangerKey
import no.nav.pia.sykefravarsstatistikk.api.auth.UnderenhetKey
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilDto
import no.nav.pia.sykefravarsstatistikk.api.dto.SamletAggregertStatistikkDto
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
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

                // TODO: Hvordan finne inneværende kvartal? Bare kall til db og sjekk høyeste årstall+kvartal?
                val årstall = 2024
                val kvartal = 4
                val sisteKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal)

                val førsteKvartal = sisteKvartal.minusKvartaler(20)

                val response: MutableList<KvartalsvisSykefraværshistorikkDto> = mutableListOf()

                val landstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkLand(
                    førsteKvartal,
                ).ifEmpty {
                    call.respond(message = "Ingen landstatistikk funnet", status = HttpStatusCode.BadRequest)
                    return@get
                }
                response.add(landstatistikk.tilDto(type = LAND.name, label = "Norge"))

                val sektorstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkSektor(
                    sektor = overordnetEnhet.sektor,
                    førsteÅrstalOgKvartal = førsteKvartal,
                ).ifEmpty {
                    call.respond(message = "Ingen sektorstatistikk funnet", status = HttpStatusCode.BadRequest)
                    return@get
                }
                response.add(sektorstatistikk.tilDto(type = SEKTOR.name, label = overordnetEnhet.sektor.kode))

                val bransje = underenhet.bransje()
                if (bransje != null) {
                    val bransjestatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkBransje(
                        bransje = bransje,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(message = "Ingen statistikk funnet or bransje '${bransje.navn}'", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    response.add(bransjestatistikk.tilDto(type = "BRANSJE", label = bransje.navn))
                } else {
                    val næringsstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkNæring(
                        næring = underenhet.næringskode.næring,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(
                            message = "Ingen statistikk funnet or næring '${underenhet.næringskode.næring}'",
                            status = HttpStatusCode.BadRequest,
                        )
                        return@get
                    }
                    response.add(næringsstatistikk.tilDto(type = "NÆRING", label = underenhet.næringskode.næring.tosifferIdentifikator))
                }

                response.add(
                    if (tilganger.harEnkeltTilgang) {
                        val virksomhetsstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkVirksomhet(
                            virksomhet = underenhet,
                            førsteÅrstalOgKvartal = førsteKvartal,
                        )
                            .ifEmpty {
                                call.respond(
                                    message = "Ingen virksomhetsstatistikk funnet for underenhet '${underenhet.orgnr}'",
                                    status = HttpStatusCode.BadRequest,
                                )
                                return@get
                            }

                        virksomhetsstatistikk.tilDto(type = VIRKSOMHET.name, label = underenhet.navn)
                    } else {
                        KvartalsvisSykefraværshistorikkDto(
                            type = VIRKSOMHET.name,
                            label = underenhet.navn,
                            kvartalsvisSykefraværsprosent = emptyList(),
                        )
                    },
                )

                response.add(
                    if (tilganger.harEnkeltTilgangOverordnetEnhet) {
                        val overordnetEnhetStatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkVirksomhet(
                            virksomhet = overordnetEnhet,
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

                call.respond(
                    status = HttpStatusCode.OK,
                    message = response,
                )
            }
        }
    }
}
