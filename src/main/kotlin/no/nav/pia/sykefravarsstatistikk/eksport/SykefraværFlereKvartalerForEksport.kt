package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.api.maskering.StatistikkUtils.kalkulerSykefraværsprosent
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Konstanter.MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import java.math.BigDecimal

data class SykefraværFlereKvartalerForEksport(
    private val umaskertSykefravær: List<UmaskertSykefraværUtenProsentForEttKvartal>,
) {
    var tapteDagsverk: BigDecimal? = null
    var muligeDagsverk: BigDecimal? = null
    var prosent: BigDecimal? = null
    var antallPersoner: Int
    var kvartaler: List<ÅrstallOgKvartal>
    val erMaskert =
        umaskertSykefravær.isNotEmpty() &&
            umaskertSykefravær.all {
                it.antallPersoner < MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
            }

    init {
        if (!erMaskert && umaskertSykefravær.isNotEmpty()) {
            tapteDagsverk = umaskertSykefravær.sumOf { it.dagsverkTeller }
            muligeDagsverk = umaskertSykefravær.sumOf { it.dagsverkNevner }
            prosent = kalkulerSykefraværsprosent(tapteDagsverk, muligeDagsverk).getOrNull()
        } else {
            tapteDagsverk = null
            muligeDagsverk = null
            prosent = null
        }
        kvartaler = umaskertSykefravær
            .map(UmaskertSykefraværUtenProsentForEttKvartal::årstallOgKvartal)
            .toList()
        antallPersoner =
            if (umaskertSykefravær.isEmpty()) {
                0
            } else {
                umaskertSykefravær.maxOf { it.antallPersoner }
            }
    }
}
