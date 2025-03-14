package no.nav.pia.sykefravarsstatistikk.persistering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ia.felles.definisjoner.bransjer.Bransje
import io.ktor.http.HttpStatusCode
import no.nav.pia.sykefravarsstatistikk.api.auth.Tilganger
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.api.dto.StatistikkJson
import no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll.Feil
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalBransje
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalLand
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæring
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalVirksomhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

class AggregertStatistikkService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
    private val importtidspunktRepository: ImporttidspunktRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

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
        logger.info(
            "Henter statistikk for næring '${næring.tosifferIdentifikator}' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}",
        )
        val sykefraværsstatistikkTilNæring =
            sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(næring = næring)
        return sykefraværsstatistikkTilNæring.filter {
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
        underenhet: Underenhet,
        tilganger: Tilganger,
    ): Either<Feil, AggregertStatistikkResponseDto> {
        val gjeldendeKvartal = importtidspunktRepository.hentNyesteImporterteKvartal()
        val førsteKvartal = gjeldendeKvartal.minusKvartaler(4)
        val prosentSiste4KvartalerTotalt = mutableListOf<StatistikkJson>()
        val prosentSiste4KvartalerGradert = mutableListOf<StatistikkJson>()
        val prosentSiste4KvartalerKorttid = mutableListOf<StatistikkJson>()
        val prosentSiste4KvartalerLangtid = mutableListOf<StatistikkJson>()
        val trendTotalt = mutableListOf<StatistikkJson>()
        val tapteDagsverkTotalt = mutableListOf<StatistikkJson>()
        val muligeDagsverkTotalt = mutableListOf<StatistikkJson>()

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
                umaskertBransjestatistikk.prosentTotaltAggregert(
                    statistikkategori = BRANSJE,
                    label = bransje.navn,
                ),
            )
            prosentSiste4KvartalerGradert.add(
                umaskertBransjestatistikk.prosentGradertAggregert(
                    statistikkategori = BRANSJE,
                    label = bransje.navn,
                ),
            )
            prosentSiste4KvartalerKorttid.add(
                umaskertBransjestatistikk.prosentKortTidAggregert(
                    statistikkategori = BRANSJE,
                    label = bransje.navn,
                ),
            )
            prosentSiste4KvartalerLangtid.add(
                umaskertBransjestatistikk.prosentLangTidAggregert(
                    statistikkategori = BRANSJE,
                    label = bransje.navn,
                ),
            )
            trendTotalt.add(
                umaskertBransjestatistikk.trendTotaltAggregert(
                    statistikkategori = BRANSJE,
                    label = bransje.navn,
                ),
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

        if (tilganger.harEnkeltTilgang) {
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
                umaskertVirksomhetsstatistikk.prosentTotaltAggregert(
                    statistikkategori = VIRKSOMHET,
                    label = underenhet.navn,
                ),
            )
            prosentSiste4KvartalerGradert.add(
                umaskertVirksomhetsstatistikk.prosentGradertAggregert(
                    statistikkategori = VIRKSOMHET,
                    label = underenhet.navn,
                ),
            )
            prosentSiste4KvartalerKorttid.add(
                umaskertVirksomhetsstatistikk.prosentKortTidAggregert(
                    statistikkategori = VIRKSOMHET,
                    label = underenhet.navn,
                ),
            )
            prosentSiste4KvartalerLangtid.add(
                umaskertVirksomhetsstatistikk.prosentLangTidAggregert(
                    statistikkategori = VIRKSOMHET,
                    label = underenhet.navn,
                ),
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

    private fun List<Sykefraværsstatistikk>.kvartalerIBeregning() = map { ÅrstallOgKvartal(it.årstall, it.kvartal) }

    private fun List<Sykefraværsstatistikk>.prosentTotaltAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentTotalt()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.prosentGradertAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentGradert()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.prosentKortTidAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentKortTid()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.prosentLangTidAggregert(
        statistikkategori: Statistikkategori,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = "%.1f".format(prosentLangTid()),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.trendTotaltAggregert(
        statistikkategori: Statistikkategori = BRANSJE,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = trendTotalt().toString(),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.tapteDagsverkTotaltAggregert(
        statistikkategori: Statistikkategori = VIRKSOMHET,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = tapteDagsverkTotalt().toString(),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )

    private fun List<Sykefraværsstatistikk>.muligeDagsverkTotaltAggregert(
        statistikkategori: Statistikkategori = VIRKSOMHET,
        label: String,
    ) = StatistikkJson(
        statistikkategori = statistikkategori.name,
        label = label,
        verdi = muligeDagsverkTotalt().toString(),
        antallPersonerIBeregningen = personerIBeregning(),
        kvartalerIBeregningen = kvartalerIBeregning(),
    )
}
