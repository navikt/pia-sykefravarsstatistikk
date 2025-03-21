package no.nav.pia.sykefravarsstatistikk.api.kopi

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.pia.sykefravarsstatistikk.api.aggregering.Aggregeringskalkulator
import no.nav.pia.sykefravarsstatistikk.api.aggregering.Aggregeringskategorier
import no.nav.pia.sykefravarsstatistikk.api.aggregering.BransjeEllerNæring
import no.nav.pia.sykefravarsstatistikk.api.aggregering.Sykefraværsdata
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import kotlin.test.Test

class AggregeringskalkulatorTest {
    @Test
    fun `aggregingskalkulator skal beregne trend`() {
        val resultat = Aggregeringskalkulator(
            sykefraværsdata = Sykefraværsdata(
                sykefravær = mapOf(
                    Aggregeringskategorier.Næring(næring = Næring("14")) to listOf(
                        UmaskertSykefraværUtenProsentForEttKvartal(
                            årstallOgKvartal = ÅrstallOgKvartal(2023, 4),
                            dagsverkTeller = 280864.7.toBigDecimal(),
                            dagsverkNevner = 3123455.8.toBigDecimal(),
                            antallPersoner = 98803,
                        ),
                        UmaskertSykefraværUtenProsentForEttKvartal(
                            årstallOgKvartal = ÅrstallOgKvartal(2024, 1),
                            dagsverkTeller = 330864.7.toBigDecimal(),
                            dagsverkNevner = 3331505.8.toBigDecimal(),
                            antallPersoner = 96403,
                        ),
                        UmaskertSykefraværUtenProsentForEttKvartal(
                            årstallOgKvartal = ÅrstallOgKvartal(2024, 2),
                            dagsverkTeller = 312606.4.toBigDecimal(),
                            dagsverkNevner = 3245624.8.toBigDecimal(),
                            antallPersoner = 96711,
                        ),
                        UmaskertSykefraværUtenProsentForEttKvartal(
                            årstallOgKvartal = ÅrstallOgKvartal(2024, 3),
                            dagsverkTeller = 311214.1.toBigDecimal(),
                            dagsverkNevner = 3782127.8.toBigDecimal(),
                            antallPersoner = 104737,
                        ),
                        UmaskertSykefraværUtenProsentForEttKvartal(
                            årstallOgKvartal = ÅrstallOgKvartal(2024, 4),
                            dagsverkTeller = 327662.8.toBigDecimal(),
                            dagsverkNevner = 3511634.6.toBigDecimal(),
                            antallPersoner = 96778,
                        ),
                    ),
                ),
            ),
            sistePubliserteKvartal = ÅrstallOgKvartal(2024, 4),
        ).trendBransjeEllerNæring(bransjeEllerNæring = BransjeEllerNæring(Næring(tosifferIdentifikator = "14")))

        resultat shouldNotBe null
        resultat.right() shouldNotBe null
        val trend = resultat.getOrNull()
        trend!!.statistikkategori shouldBe Statistikkategori.NÆRING
        trend.label shouldBe "Produksjon av klær"
        trend.verdi shouldBe "0.3"
        trend.antallPersonerIBeregningen shouldBe 195581
        trend.kvartalerIBeregningen shouldBe listOf(
            ÅrstallOgKvartal(2024, 4),
            ÅrstallOgKvartal(2023, 4),
        )
    }
}
