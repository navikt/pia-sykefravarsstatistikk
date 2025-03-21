package no.nav.pia.sykefravarsstatistikk.api.aggregering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal

data class Trendkalkulator(
    var datagrunnlag: List<UmaskertSykefraværUtenProsentForEttKvartal>,
    var sistePubliserteKvartal: ÅrstallOgKvartal,
) {
    fun kalkulerTrend(): Either<UtilstrekkeligData, Trend> {
        val ettÅrSiden = sistePubliserteKvartal.minusEttÅr()
        val nyesteSykefravær = hentUtKvartal(datagrunnlag, sistePubliserteKvartal)
        val sykefraværetEttÅrSiden = hentUtKvartal(datagrunnlag, ettÅrSiden)
        if (nyesteSykefravær == null || sykefraværetEttÅrSiden == null) {
            return UtilstrekkeligData(
                "Mangler data for $sistePubliserteKvartal og/eller $ettÅrSiden",
            ).left()
        }
        val nyesteSykefraværsprosent = nyesteSykefravær.kalkulerSykefraværsprosent().getOrNull()
        val sykefraværsprosentEttÅrSiden = sykefraværetEttÅrSiden.kalkulerSykefraværsprosent().getOrNull()
        if (nyesteSykefraværsprosent == null || sykefraværsprosentEttÅrSiden == null) {
            return UtilstrekkeligData("Feil i utregningen av sykefraværsprosenten, kan ikke regne ut trendverdi.").left()
        }
        val trendverdi = nyesteSykefraværsprosent.subtract(sykefraværsprosentEttÅrSiden)
        val antallTilfeller = (
            nyesteSykefravær.antallPersoner +
                sykefraværetEttÅrSiden.antallPersoner
        )
        return Trend(trendverdi, antallTilfeller, listOf(sistePubliserteKvartal, ettÅrSiden)).right()
    }
}

fun hentUtKvartal(
    sykefravær: Collection<UmaskertSykefraværUtenProsentForEttKvartal>,
    kvartal: ÅrstallOgKvartal,
): UmaskertSykefraværUtenProsentForEttKvartal? = sykefravær.find { it.årstallOgKvartal == kvartal }
