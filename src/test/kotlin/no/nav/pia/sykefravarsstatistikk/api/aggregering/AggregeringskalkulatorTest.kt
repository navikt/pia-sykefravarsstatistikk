package no.nav.pia.sykefravarsstatistikk.api.aggregering

import arrow.core.right
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetSykehjemMedTilgang
import kotlin.test.Test

class AggregeringskalkulatorTest {
    @Test
    fun `aggregingskalkulator skal beregne trendTotalt`() {
        val resultat = Aggregeringskalkulator(
            sykefraværsdata = lagSykefraværsdata(Aggregeringskategorier.Næring(næring = Næring("14"))),
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

    @Test
    fun `aggregingskalkulator skal beregne tapteDagsverkTotalt`() {
        val resultat = Aggregeringskalkulator(
            sykefraværsdata = lagSykefraværsdata(
                Aggregeringskategorier.Virksomhet(
                    virksomhet = underenhetSykehjemMedTilgang.somNæringsdrivende()
                )),
            sistePubliserteKvartal = ÅrstallOgKvartal(2024, 4),
        ).tapteDagsverkVirksomhet(bedriftsnavn = underenhetSykehjemMedTilgang.navn)

        resultat shouldNotBe null
        resultat.right() shouldNotBe null
        val tapteDagsverkTotalt = resultat.getOrNull()
        tapteDagsverkTotalt!!.statistikkategori shouldBe Statistikkategori.VIRKSOMHET
        tapteDagsverkTotalt.label shouldBe underenhetSykehjemMedTilgang.navn
        tapteDagsverkTotalt.verdi shouldBe "1282348.0"
        tapteDagsverkTotalt.antallPersonerIBeregningen shouldBe 104737
        tapteDagsverkTotalt.kvartalerIBeregningen shouldBe listOf(
            ÅrstallOgKvartal(2024, 1),
            ÅrstallOgKvartal(2024, 2),
            ÅrstallOgKvartal(2024, 3),
            ÅrstallOgKvartal(2024, 4),
        )
    }

    private fun lagSykefraværsdata(aggregeringskategori: Aggregeringskategorier) =
        Sykefraværsdata(
            sykefravær = mapOf(
                aggregeringskategori to listOf(
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
        )
}
