package no.nav.pia.sykefravarsstatistikk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.pia.sykefravarsstatistikk.api.auth.OverordnetEnhetKey
import no.nav.pia.sykefravarsstatistikk.api.auth.TilgangerKey
import no.nav.pia.sykefravarsstatistikk.api.auth.UnderenhetKey
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.muligeDagsverkTotaltAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentGradertAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentKortTidAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentLangTidAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentTotaltAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.tapteDagsverkTotaltAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.trendTotaltAggregert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilDto
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
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
                        call.respond(
                            message = "Ingen statistikk funnet for bransje '${bransje.navn}'",
                            status = HttpStatusCode.BadRequest,
                        )
                        return@get
                    }
                    response.add(bransjestatistikk.tilDto(type = "BRANSJE", label = bransje.navn))
                } else {
                    val næringsstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkNæring(
                        næring = underenhet.næringskode.næring,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(
                            message = "Ingen statistikk funnet for næring '${underenhet.næringskode.næring}'",
                            status = HttpStatusCode.BadRequest,
                        )
                        return@get
                    }
                    response.add(
                        næringsstatistikk.tilDto(
                            type = "NÆRING",
                            label = underenhet.næringskode.næring.tosifferIdentifikator,
                        ),
                    )
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

        route("/siste4kvartaler/aggregert") {
            get {
                logger.info("Henter aggregert statistikk siste fire kvartaler")
                val tilganger = call.attributes[TilgangerKey]
                val underenhet = call.attributes[UnderenhetKey]
                val overordnetEnhet = call.attributes[OverordnetEnhetKey]
                val årstall = 2024
                val kvartal = 4
                val inneværendeKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal)
                val førsteKvartal = inneværendeKvartal.minusKvartaler(4) // 4 siste kvartaler

                val prosentSiste4KvartalerTotalt = mutableListOf<AggregertStatistikkDto>()
                val prosentSiste4KvartalerGradert = mutableListOf<AggregertStatistikkDto>()
                val prosentSiste4KvartalerKorttid = mutableListOf<AggregertStatistikkDto>()
                val prosentSiste4KvartalerLangtid = mutableListOf<AggregertStatistikkDto>()
                val trendTotalt = mutableListOf<AggregertStatistikkDto>()
                val tapteDagsverkTotalt = mutableListOf<AggregertStatistikkDto>()
                val muligeDagsverkTotalt = mutableListOf<AggregertStatistikkDto>()

                val landstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkLand(
                    førsteÅrstalOgKvartal = førsteKvartal,
                ).ifEmpty {
                    call.respond(message = "Ingen landstatistikk funnet", status = HttpStatusCode.BadRequest)
                    return@get
                }
                prosentSiste4KvartalerTotalt.add(landstatistikk.prosentTotaltAggregert(statistikkategori = LAND, label = "Norge"))

                val bransje = underenhet.bransje()
                if (bransje != null) {
                    val bransjestatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkBransje(
                        bransje = bransje,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(message = "Ingen statistikk funnet or bransje '${bransje.navn}'", status = HttpStatusCode.BadRequest)
                        return@get
                    }
                    prosentSiste4KvartalerTotalt.add(
                        bransjestatistikk.prosentTotaltAggregert(statistikkategori = BRANSJE, label = bransje.navn),
                    )
                    prosentSiste4KvartalerGradert.add(
                        bransjestatistikk.prosentGradertAggregert(statistikkategori = BRANSJE, label = bransje.navn),
                    )
                    prosentSiste4KvartalerKorttid.add(
                        bransjestatistikk.prosentKortTidAggregert(statistikkategori = BRANSJE, label = bransje.navn),
                    )
                    prosentSiste4KvartalerLangtid.add(
                        bransjestatistikk.prosentLangTidAggregert(statistikkategori = BRANSJE, label = bransje.navn),
                    )
                    trendTotalt.add(
                        bransjestatistikk.trendTotaltAggregert(statistikkategori = BRANSJE, label = bransje.navn),
                    )
                } else {
                    val næring = underenhet.næringskode.næring
                    val næringsstatistikk = sykefraværsstatistikkService.hentSykefraværsstatistikkNæring(
                        næring = næring,
                        førsteÅrstalOgKvartal = førsteKvartal,
                    ).ifEmpty {
                        call.respond(
                            message = "Ingen statistikk funnet or næring '$næring'",
                            status = HttpStatusCode.BadRequest,
                        )
                        return@get
                    }
                    prosentSiste4KvartalerTotalt.add(
                        næringsstatistikk.prosentTotaltAggregert(statistikkategori = NÆRING, label = næring.tosifferIdentifikator),
                    )
                    prosentSiste4KvartalerGradert.add(
                        næringsstatistikk.prosentGradertAggregert(statistikkategori = NÆRING, label = næring.tosifferIdentifikator),
                    )
                    prosentSiste4KvartalerKorttid.add(
                        næringsstatistikk.prosentKortTidAggregert(statistikkategori = NÆRING, label = næring.tosifferIdentifikator),
                    )
                    prosentSiste4KvartalerLangtid.add(
                        næringsstatistikk.prosentLangTidAggregert(statistikkategori = NÆRING, label = næring.tosifferIdentifikator),
                    )
                    trendTotalt.add(
                        næringsstatistikk.trendTotaltAggregert(statistikkategori = NÆRING, label = næring.tosifferIdentifikator),
                    )
                }

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

                    prosentSiste4KvartalerTotalt.add(
                        virksomhetsstatistikk.prosentTotaltAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
                    )
                    prosentSiste4KvartalerGradert.add(
                        virksomhetsstatistikk.prosentGradertAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
                    )
                    prosentSiste4KvartalerKorttid.add(
                        virksomhetsstatistikk.prosentKortTidAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
                    )
                    prosentSiste4KvartalerLangtid.add(
                        virksomhetsstatistikk.prosentLangTidAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
                    )
                    tapteDagsverkTotalt.add(
                        virksomhetsstatistikk.tapteDagsverkTotaltAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
                    )
                    muligeDagsverkTotalt.add(
                        virksomhetsstatistikk.muligeDagsverkTotaltAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
                    )
                }

                call.respond(
                    HttpStatusCode.OK,
                    message = AggregertStatistikkResponseDto(
                        prosentSiste4KvartalerTotalt = prosentSiste4KvartalerTotalt,
                        prosentSiste4KvartalerGradert = prosentSiste4KvartalerGradert,
                        prosentSiste4KvartalerKorttid = prosentSiste4KvartalerKorttid,
                        prosentSiste4KvartalerLangtid = prosentSiste4KvartalerLangtid,
                        trendTotalt = trendTotalt,
                        tapteDagsverkTotalt = tapteDagsverkTotalt,
                        muligeDagsverkTotalt = muligeDagsverkTotalt,
                    ),
                )
            }
        }
    }
}
