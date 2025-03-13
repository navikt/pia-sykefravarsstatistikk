package no.nav.pia.sykefravarsstatistikk.persistering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ia.felles.definisjoner.bransjer.Bransje
import io.ktor.http.HttpStatusCode
import no.nav.pia.sykefravarsstatistikk.api.auth.Tilganger
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalIBeregning
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilDto
import no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll.Feil
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalBransje
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalLand
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæring
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalSektor
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalVirksomhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

class SykefraværsstatistikkService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreSykefraværsstatistikk(statistikk: List<SykefraværsstatistikkDto>) {
        logger.info("Starter lagring av statistikk, antall statistikk som skal lagres: '${statistikk.size}'")
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(statistikk)
    }

    private fun hentSykefraværsstatistikkVirksomhet(
        virksomhet: Virksomhet,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalVirksomhet> {
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

    private fun hentSykefraværsstatistikkBransje(
        bransje: Bransje,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalBransje> {
        logger.info("Henter statistikk for bransje '$bransje' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilBransje = sykefraværsstatistikkRepository.hentSykefraværsstatistikkBransje(
            bransje = bransje,
        )
        return sykefraværsstatistikkTilBransje.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkNæring(
        næring: Næring,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalNæring> {
        logger.info("Henter statistikk for næring '${næring.tosifferIdentifikator}' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilNæring =
            sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(næring = næring)
        return sykefraværsstatistikkTilNæring.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkSektor(
        sektor: Sektor,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalSektor> {
        logger.info("Henter statistikk for sektor '$sektor' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilSektor = sykefraværsstatistikkRepository.hentSykefraværsstatistikkSektor(
            sektor = sektor,
        )
        return sykefraværsstatistikkTilSektor.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkLand(
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalLand> {
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

        val umaskertLandsstatistikk = hentSykefraværsstatistikkLand(
            førsteÅrstalOgKvartal = førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen landstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }

        prosentSiste4KvartalerTotalt.add(
            umaskertLandsstatistikk.prosentTotaltAggregert(
                statistikkategori = LAND,
                label = "Norge",
            ),
        )

        val bransje = underenhet.bransje()
        if (bransje != null) {
            val umaskertBransjestatistikk = hentSykefraværsstatistikkBransje(
                bransje = bransje,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen statistikk funnet for bransje '${bransje.navn}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            prosentSiste4KvartalerTotalt.add(
                umaskertBransjestatistikk.prosentTotaltAggregert(statistikkategori = BRANSJE, label = bransje.navn),
            )
            prosentSiste4KvartalerGradert.add(
                umaskertBransjestatistikk.prosentGradertAggregert(statistikkategori = BRANSJE, label = bransje.navn),
            )
            prosentSiste4KvartalerKorttid.add(
                umaskertBransjestatistikk.prosentKortTidAggregert(statistikkategori = BRANSJE, label = bransje.navn),
            )
            prosentSiste4KvartalerLangtid.add(
                umaskertBransjestatistikk.prosentLangTidAggregert(statistikkategori = BRANSJE, label = bransje.navn),
            )
            trendTotalt.add(
                umaskertBransjestatistikk.trendTotaltAggregert(statistikkategori = BRANSJE, label = bransje.navn),
            )
        } else {
            val næring = underenhet.næringskode.næring
            val umaskertNæringsstatistikk = hentSykefraværsstatistikkNæring(
                næring = næring,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen statistikk funnet or næring '$næring'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            prosentSiste4KvartalerTotalt.add(
                umaskertNæringsstatistikk.prosentTotaltAggregert(
                    statistikkategori = NÆRING,
                    label = næring.tosifferIdentifikator,
                ),
            )
            prosentSiste4KvartalerGradert.add(
                umaskertNæringsstatistikk.prosentGradertAggregert(
                    statistikkategori = NÆRING,
                    label = næring.tosifferIdentifikator,
                ),
            )
            prosentSiste4KvartalerKorttid.add(
                umaskertNæringsstatistikk.prosentKortTidAggregert(
                    statistikkategori = NÆRING,
                    label = næring.tosifferIdentifikator,
                ),
            )
            prosentSiste4KvartalerLangtid.add(
                umaskertNæringsstatistikk.prosentLangTidAggregert(
                    statistikkategori = NÆRING,
                    label = næring.tosifferIdentifikator,
                ),
            )
            trendTotalt.add(
                umaskertNæringsstatistikk.trendTotaltAggregert(
                    statistikkategori = NÆRING,
                    label = næring.tosifferIdentifikator,
                ),
            )
        }

        if (harEnkeltTilgang) {
            val umaskertVirksomhetsstatistikk = hentSykefraværsstatistikkVirksomhet(
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
                umaskertVirksomhetsstatistikk.prosentTotaltAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
            )
            prosentSiste4KvartalerGradert.add(
                umaskertVirksomhetsstatistikk.prosentGradertAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
            )
            prosentSiste4KvartalerKorttid.add(
                umaskertVirksomhetsstatistikk.prosentKortTidAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
            )
            prosentSiste4KvartalerLangtid.add(
                umaskertVirksomhetsstatistikk.prosentLangTidAggregert(statistikkategori = VIRKSOMHET, label = underenhet.navn),
            )
            tapteDagsverkTotalt.add(
                umaskertVirksomhetsstatistikk.tapteDagsverkTotaltAggregert(
                    statistikkategori = VIRKSOMHET,
                    label = underenhet.navn,
                ),
            )
            muligeDagsverkTotalt.add(
                umaskertVirksomhetsstatistikk.muligeDagsverkTotaltAggregert(
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

        val umaskertLandstatistikk = hentSykefraværsstatistikkLand(
            førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen landstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }
        response.add(umaskertLandstatistikk.tilDto(type = LAND.name, label = "Norge"))

        val umaskertSektorstatistikk = hentSykefraværsstatistikkSektor(
            sektor = overordnetEnhet.sektor,
            førsteÅrstalOgKvartal = førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen sektorstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }
        response.add(umaskertSektorstatistikk.tilDto(type = SEKTOR.name, label = overordnetEnhet.sektor.kode))

        val bransje = underenhet.bransje()
        if (bransje != null) {
            val umaskertBransjestatistikk = hentSykefraværsstatistikkBransje(
                bransje = bransje,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen bransjestatistikk funnet for bransje '${bransje.navn}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            response.add(umaskertBransjestatistikk.tilDto(type = "BRANSJE", label = bransje.navn))
        } else {
            val umaskertNæringsstatistikk = hentSykefraværsstatistikkNæring(
                næring = underenhet.næringskode.næring,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen næringsstatistikk funnet for næring " +
                        "med navn: '${underenhet.næringskode.næring.navn}' " +
                        "og kode: '${underenhet.næringskode.næring.tosifferIdentifikator}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            response.add(
                umaskertNæringsstatistikk.tilDto(
                    type = "NÆRING",
                    label = underenhet.næringskode.næring.tosifferIdentifikator,
                ),
            )
        }

        if (tilganger.harEnkeltTilgang) {
            val umaskertVirksomhetsstatistikk = hentSykefraværsstatistikkVirksomhet(
                virksomhet = underenhet,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen virksomhetsstatistikk funnet for underenhet '${underenhet.orgnr}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }

            response.add(umaskertVirksomhetsstatistikk.tilDto(type = VIRKSOMHET.name, label = underenhet.navn))
        }

        if (tilganger.harEnkeltTilgangOverordnetEnhet) {
            val umaskertVirksomhetsstatistikk = hentSykefraværsstatistikkVirksomhet(
                virksomhet = overordnetEnhet,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen virksomhetsstatistikk funnet for overordnet enhet '${overordnetEnhet.orgnr}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            response.add(
                umaskertVirksomhetsstatistikk.tilDto(type = "OVERORDNET_ENHET", label = overordnetEnhet.navn),
            )
        }

        return response.toList().right()
    }

    // TODO: Kopier fra Sykefraværsstatistikk-api
    private fun List<BigDecimal>.average(): BigDecimal {
        if (this.isEmpty()) {
            return BigDecimal.ZERO
        }

        val sum = this.reduce { acc, value -> acc.add(value) }
        return sum.divide(BigDecimal(this.size), 10, RoundingMode.HALF_UP)
    }

    private fun List<Sykefraværsstatistikk>.muligeDagsverkTotalt() = sumOf { it.muligeDagsverk }

    private fun List<Sykefraværsstatistikk>.tapteDagsverkTotalt() = sumOf { it.tapteDagsverk }

    private fun List<Sykefraværsstatistikk>.prosentLangTid() = map { it.prosent }.average()

    private fun List<Sykefraværsstatistikk>.prosentKortTid() = map { it.prosent }.average()

    private fun List<Sykefraværsstatistikk>.prosentGradert() = tapteDagsverkTotalt() / muligeDagsverkTotalt() * BigDecimal(100)

    private fun List<Sykefraværsstatistikk>.personerIBeregning() = map { it.antallPersoner }.average().toInt()

    private fun List<Sykefraværsstatistikk>.trendTotalt() = -1.0

    private fun List<Sykefraværsstatistikk>.prosentTotalt() = tapteDagsverkTotalt() / muligeDagsverkTotalt() * BigDecimal(100)

    private fun List<Sykefraværsstatistikk>.kvartalerIBeregning() = map { KvartalIBeregning(it.årstall, it.kvartal) }

    private fun List<Sykefraværsstatistikk>.prosentTotaltAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentTotalt()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.prosentGradertAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentGradert()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.prosentKortTidAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentKortTid()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.prosentLangTidAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentLangTid()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.trendTotaltAggregert(
        statistikkategori: Statistikkategori = BRANSJE,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = trendTotalt().toString(),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.tapteDagsverkTotaltAggregert(
        statistikkategori: Statistikkategori = VIRKSOMHET,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = tapteDagsverkTotalt().toString(),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.muligeDagsverkTotaltAggregert(
        statistikkategori: Statistikkategori = VIRKSOMHET,
        label: String,
    ) = AggregertStatistikkDto(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = muligeDagsverkTotalt().toString(),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )
}
