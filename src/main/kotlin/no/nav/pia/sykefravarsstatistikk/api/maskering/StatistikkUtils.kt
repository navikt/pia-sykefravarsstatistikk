package no.nav.pia.sykefravarsstatistikk.api.maskering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.pia.sykefravarsstatistikk.exceptions.Statistikkfeil
import java.math.BigDecimal
import java.math.RoundingMode

object StatistikkUtils {
    const val ANTALL_SIFRE_I_UTREGNING = 3
    const val ANTALL_SIFRE_I_RESULTAT = 1

    /**
     * Source of trouth for kalkulering av sykefraværsprosent basert på tapte dagsverk og mulige
     * dagsverk.
     */
    fun kalkulerSykefraværsprosent(
        dagsverkTeller: BigDecimal?,
        dagsverkNevner: BigDecimal?,
    ): Either<Statistikkfeil, BigDecimal> {
        if (dagsverkTeller == null || dagsverkNevner == null || dagsverkNevner.compareTo(BigDecimal.ZERO) == 0) {
            return Statistikkfeil(
                "Kan ikke regne ut prosent når antall dagsverk i nevner er lik $dagsverkNevner",
            ).left()
        }

        return dagsverkTeller
            .divide(dagsverkNevner, ANTALL_SIFRE_I_UTREGNING, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(ANTALL_SIFRE_I_RESULTAT, RoundingMode.HALF_UP).right()
    }
}
