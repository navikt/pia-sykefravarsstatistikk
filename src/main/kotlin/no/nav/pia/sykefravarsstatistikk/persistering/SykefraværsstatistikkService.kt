package no.nav.pia.sykefravarsstatistikk.persistering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ia.felles.definisjoner.bransjer.Bransje
import io.ktor.http.HttpStatusCode
import no.nav.pia.sykefravarsstatistikk.api.auth.Tilganger
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
import no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll.Feil
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkBransje
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkLand
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkNæring
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkSektor
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkVirksomhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SykefraværsstatistikkService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreSykefraværsstatistikk(statistikk: List<SykefraværsstatistikkDto>) {
        logger.info("Starter lagring av statistikk, antall statistikk som skal lagres: '${statistikk.size}'")
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(statistikk)
    }

    fun hentSykefraværsstatistikkVirksomhet(
        virksomhet: Virksomhet,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkVirksomhet> {
        logger.info(
            "Henter statistikk for virksomhet med orgnr: '${virksomhet.orgnr}' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}",
        )
        val sykefraværsstatistikkTilVirksomhet = sykefraværsstatistikkRepository.hentSykefraværsstatistikkVirksomhet(
            virksomhet = virksomhet,
        )
        return sykefraværsstatistikkTilVirksomhet.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkBransje(
        bransje: Bransje,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkBransje> {
        logger.info("Henter statistikk for bransje '$bransje' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilBransje = sykefraværsstatistikkRepository.hentSykefraværsstatistikkBransje(
            bransje = bransje,
        )
        return sykefraværsstatistikkTilBransje.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkNæring(
        næring: Næring,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkNæring> {
        logger.info("Henter statistikk for næring '$næring' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilNæring = sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(næring = næring)
        return sykefraværsstatistikkTilNæring.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkSektor(
        sektor: Sektor,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkSektor> {
        logger.info("Henter statistikk for sektor '$sektor' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilSektor = sykefraværsstatistikkRepository.hentSykefraværsstatistikkSektor(
            sektor = sektor,
        )
        return sykefraværsstatistikkTilSektor.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkLand(førsteÅrstalOgKvartal: ÅrstallOgKvartal): List<SykefraværsstatistikkLand> {
        logger.info("Henter statistikk for land  fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkLand = sykefraværsstatistikkRepository.hentSykefraværsstatistikkLand()
        return sykefraværsstatistikkLand.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentAggregertStatistikk(
        inneværendeKvartal: ÅrstallOgKvartal,
        underenhet: Underenhet,
        harEnkeltTilgang: Boolean,
    ): Either<Feil, AggregertStatistikkResponseDto> {
        val førsteKvartal = inneværendeKvartal.minusKvartaler(4) // 4 siste kvartaler
        val prosentSiste4KvartalerTotalt = mutableListOf<AggregertStatistikkDto>()
        val prosentSiste4KvartalerGradert = mutableListOf<AggregertStatistikkDto>()
        val prosentSiste4KvartalerKorttid = mutableListOf<AggregertStatistikkDto>()
        val prosentSiste4KvartalerLangtid = mutableListOf<AggregertStatistikkDto>()
        val trendTotalt = mutableListOf<AggregertStatistikkDto>()
        val tapteDagsverkTotalt = mutableListOf<AggregertStatistikkDto>()
        val muligeDagsverkTotalt = mutableListOf<AggregertStatistikkDto>()

        // Todo: Vi lager liste av dtoer, burde egt lage domeneobjekter og kjøre tilDto() i route

        val landstatistikk = hentSykefraværsstatistikkLand(
            førsteÅrstalOgKvartal = førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen landstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }

        prosentSiste4KvartalerTotalt.add(landstatistikk.prosentTotaltAggregert(statistikkategori = LAND, label = "Norge"))

        val bransje = underenhet.bransje()
        if (bransje != null) {
            val bransjestatistikk = hentSykefraværsstatistikkBransje(
                bransje = bransje,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen statistikk funnet for bransje '${bransje.navn}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
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
            val næringsstatistikk = hentSykefraværsstatistikkNæring(
                næring = næring,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen statistikk funnet or næring '$næring'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
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

        if (harEnkeltTilgang) {
            val virksomhetsstatistikk = hentSykefraværsstatistikkVirksomhet(
                virksomhet = underenhet,
                førsteÅrstalOgKvartal = førsteKvartal,
            )
                .ifEmpty {
                    return Feil(
                        feilmelding = "Ingen virksomhetsstatistikk funnet for underenhet '${underenhet.orgnr}'",
                        httpStatusCode = HttpStatusCode.BadRequest,
                    ).left()
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
                virksomhetsstatistikk.muligeDagsverkTotaltAggregert(
                    statistikkategori = VIRKSOMHET,
                    label = underenhet.navn,
                ),
            )
        }

        return AggregertStatistikkResponseDto(
            prosentSiste4KvartalerTotalt = prosentSiste4KvartalerTotalt,
            prosentSiste4KvartalerGradert = prosentSiste4KvartalerGradert,
            prosentSiste4KvartalerKorttid = prosentSiste4KvartalerKorttid,
            prosentSiste4KvartalerLangtid = prosentSiste4KvartalerLangtid,
            trendTotalt = trendTotalt,
            tapteDagsverkTotalt = tapteDagsverkTotalt,
            muligeDagsverkTotalt = muligeDagsverkTotalt,
        ).right()
    }

    fun hentKvartalsvisStatistikk(
        sisteKvartal: ÅrstallOgKvartal,
        overordnetEnhet: OverordnetEnhet,
        underenhet: Underenhet,
        tilganger: Tilganger,
    ): Either<Feil, List<KvartalsvisSykefraværshistorikkDto>> {
        // Todo: Vi lager liste av dtoer, burde egt lage domeneobjekter og kjøre tilDto() i route

        val førsteKvartal = sisteKvartal.minusKvartaler(20)

        val response: MutableList<KvartalsvisSykefraværshistorikkDto> = mutableListOf()

        val landstatistikk = hentSykefraværsstatistikkLand(
            førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen landstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }
        response.add(landstatistikk.tilDto(type = LAND.name, label = "Norge"))

        val sektorstatistikk = hentSykefraværsstatistikkSektor(
            sektor = overordnetEnhet.sektor,
            førsteÅrstalOgKvartal = førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen sektorstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }
        response.add(sektorstatistikk.tilDto(type = SEKTOR.name, label = overordnetEnhet.sektor.kode))

        val bransje = underenhet.bransje()
        if (bransje != null) {
            val bransjestatistikk = hentSykefraværsstatistikkBransje(
                bransje = bransje,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen bransjestatistikk funnet for bransje '${bransje.navn}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            response.add(bransjestatistikk.tilDto(type = "BRANSJE", label = bransje.navn))
        } else {
            val næringsstatistikk = hentSykefraværsstatistikkNæring(
                næring = underenhet.næringskode.næring,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen næringsstatistikk funnet for næring '${underenhet.næringskode.næring}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
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
                val virksomhetsstatistikk = hentSykefraværsstatistikkVirksomhet(
                    virksomhet = underenhet,
                    førsteÅrstalOgKvartal = førsteKvartal,
                ).ifEmpty {
                    return Feil(
                        feilmelding = "Ingen virksomhetsstatistikk funnet for underenhet '${underenhet.orgnr}'",
                        httpStatusCode = HttpStatusCode.BadRequest,
                    ).left()
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
                val overordnetEnhetStatistikk = hentSykefraværsstatistikkVirksomhet(
                    virksomhet = overordnetEnhet,
                    førsteÅrstalOgKvartal = førsteKvartal,
                ).ifEmpty {
                    return Feil(
                        feilmelding = "Ingen virksomhetsstatistikk funnet for overordnet enhet '${overordnetEnhet.orgnr}'",
                        httpStatusCode = HttpStatusCode.BadRequest,
                    ).left()
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
        return response.toList().right()
    }
}
