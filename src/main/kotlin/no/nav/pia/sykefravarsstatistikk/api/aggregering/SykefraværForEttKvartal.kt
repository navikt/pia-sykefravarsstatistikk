package no.nav.pia.sykefravarsstatistikk.api.aggregering

import no.nav.pia.sykefravarsstatistikk.api.maskering.MaskerbartSykefravær
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import java.math.BigDecimal
import java.util.Objects

open class SykefraværForEttKvartal(
    @Transient
    val årstallOgKvartal: ÅrstallOgKvartal?,
    tapteDagsverk: BigDecimal?,
    muligeDagsverk: BigDecimal?,
    @Transient
    open val antallPersoner: Int,
) : MaskerbartSykefravær(
        tapteDagsverk,
        muligeDagsverk,
        antallPersoner,
        årstallOgKvartal != null && tapteDagsverk != null && muligeDagsverk != null,
    ),
    Comparable<SykefraværForEttKvartal> {
    val kvartal: Int
        get() = årstallOgKvartal?.kvartal ?: 0
    val Årstall: Int
        get() = årstallOgKvartal?.årstall ?: 0

    override fun compareTo(other: SykefraværForEttKvartal) = compareValuesBy(this, other) { it.årstallOgKvartal }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is SykefraværForEttKvartal) {
            return false
        }
        if (!super.equals(other)) {
            return false
        }
        val erÅrstallOgKvartalLike = årstallOgKvartal == other.årstallOgKvartal
        val erProsentLike = prosent == other.prosent
        val erTapteDagsverkLike = tapteDagsverk == other.tapteDagsverk
        val erMuligeDagsverkLike = muligeDagsverk == other.muligeDagsverk

        return erÅrstallOgKvartalLike && erProsentLike && erTapteDagsverkLike && erMuligeDagsverkLike
    }

    override fun hashCode(): Int = Objects.hash(super.hashCode(), årstallOgKvartal)
}
