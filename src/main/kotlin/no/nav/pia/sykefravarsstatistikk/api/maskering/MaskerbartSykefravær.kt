package no.nav.pia.sykefravarsstatistikk.api.maskering

import no.nav.pia.sykefravarsstatistikk.domene.Konstanter
import java.math.BigDecimal
import java.math.RoundingMode

open class MaskerbartSykefravær(
    tapteDagsverk: BigDecimal?,
    muligeDagsverk: BigDecimal?,
    antallPersoner: Int,
    harSykefraværData: Boolean,
) {
    var prosent: BigDecimal? = null
    var tapteDagsverk: BigDecimal? = null
    var muligeDagsverk: BigDecimal? = null
    val erMaskert: Boolean = (
        harSykefraværData &&
            antallPersoner < Konstanter.MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
    )

    init {
        if (!erMaskert && harSykefraværData) {
            prosent = StatistikkUtils.kalkulerSykefraværsprosent(tapteDagsverk!!, muligeDagsverk!!).getOrNull()
            this.tapteDagsverk = tapteDagsverk.setScale(1, RoundingMode.HALF_UP)
            this.muligeDagsverk = muligeDagsverk.setScale(1, RoundingMode.HALF_UP)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MaskerbartSykefravær

        if (prosent != other.prosent) return false
        if (tapteDagsverk != other.tapteDagsverk) return false
        if (muligeDagsverk != other.muligeDagsverk) return false

        return true
    }

    override fun hashCode(): Int {
        var result = prosent?.hashCode() ?: 0
        result = 31 * result + (tapteDagsverk?.hashCode() ?: 0)
        result = 31 * result + (muligeDagsverk?.hashCode() ?: 0)
        return result
    }
}
