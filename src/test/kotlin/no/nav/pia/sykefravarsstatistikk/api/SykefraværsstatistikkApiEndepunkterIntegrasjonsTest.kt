package no.nav.pia.sykefravarsstatistikk.api

import ia.felles.definisjoner.bransjer.Bransje
import io.kotest.inspectors.shouldForNone
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetSykehjemMedTilgang
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.BeforeTest
import kotlin.test.Test

class SykefraværsstatistikkApiEndepunkterIntegrasjonsTest {
    @BeforeTest
    fun cleanUp() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
        }
    }

    @Test
    fun `autotest som utledder sykefraværsprosent`() {
        sykefraværsprosent(
            tapteDagsverk = 8894430.toBigDecimal(),
            muligeDagsverk = 142947000.toBigDecimal(),
        ) shouldBe 6.2
    }

    @Test
    fun `Sjekk format og data fra kvartalsvis statistikk`() {
        val underenhet: Underenhet = underenhetSykehjemMedTilgang

        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhet.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )
            lagLandStatistikkTestCase()
            lagSektorStatistikkTestCase()
            lagBransjeStatistikkTestCase()
            lagVirksomhetStatistikkTestCase()

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhet.orgnr,
                config = withToken(),
            )

            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.LAND.name }
            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.SEKTOR.name }
            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.BRANSJE.name }

            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.VIRKSOMHET.name }

            val underenhetStatistikk =
                kvartalsvisStatistikk.firstOrNull { it.type == Statistikkategori.VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            /*
            Expected :"Underenhet Sykehjem Med Tilgang (privat sektor)"
            Actual   :"Underenhet Med Enkelrettighet Bransje Sykehjem"
             */
            underenhetStatistikk.label shouldBe underenhetSykehjemMedTilgang.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 4

            kvartalsvisStatistikk.shouldForNone { statistikk -> statistikk.type shouldBe Statistikkategori.OVERORDNET_ENHET.name }
        }
    }

    private fun lagVirksomhetStatistikkTestCase() {
        // Data er tatt fra sykefravar_statistikk_virksomhet_med_gradering (i dev)
        // tapteDv = sykefravar_statistikk_virksomhet_med_gradering.tapte_dagsverk
        //  men det kunne også har vært sum av tapteDV i tapteDagsverkMedVarighet lista
        // tapteDagsverGradert = = sykefravar_statistikk_virksomhet_med_gradering.tapte_dagsverk_gradert_sykemelding
        // antallPersoner = = sykefravar_statistikk_virksomhet_med_gradering.antall_personer
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = underenhetSykehjemMedTilgang.orgnr,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
            tapteDagsverk = 440.0.toBigDecimal(),
            muligeDagsverk = 1254.0.toBigDecimal(),
            antallPersoner = 22,
            tapteDagsverGradert = 387.710000.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 21.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 37.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "C",
                    tapteDagsverk = 24.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 27.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 67.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 2.0.toBigDecimal(),
                ),
            ),
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = underenhetSykehjemMedTilgang.orgnr,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
            tapteDagsverk = 85.0.toBigDecimal(),
            muligeDagsverk = 1653.0.toBigDecimal(),
            antallPersoner = 29,
            tapteDagsverGradert = 79.258500.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 3.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 11.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 12.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 13.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 6.0.toBigDecimal(),
                ),
            ),
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = underenhetSykehjemMedTilgang.orgnr,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
            tapteDagsverk = 118.0.toBigDecimal(),
            muligeDagsverk = 1197.0.toBigDecimal(),
            antallPersoner = 21,
            tapteDagsverGradert = 100.936000.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 2.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 11.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 18.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 14.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 13.0.toBigDecimal(),
                ),
            ),
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = underenhetSykehjemMedTilgang.orgnr,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
            tapteDagsverk = 41.0.toBigDecimal(),
            muligeDagsverk = 1140.0.toBigDecimal(),
            antallPersoner = 20,
            tapteDagsverGradert = 15.850700.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 5.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 3.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "C",
                    tapteDagsverk = 5.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 3.0.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 3.0.toBigDecimal(),
                ),
            ),
        )
    }

    fun lagBransjeStatistikkTestCase() {
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.BRANSJE,
            kode = Bransje.SYKEHJEM.navn,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
            tapteDagsverk = 330864.7.toBigDecimal(),
            muligeDagsverk = 3331505.8.toBigDecimal(),
            prosent = 9.9.toBigDecimal(),
            antallPersoner = 1000,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.BRANSJE,
            kode = Bransje.SYKEHJEM.navn,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
            tapteDagsverk = 312606.4.toBigDecimal(),
            muligeDagsverk = 3245624.8.toBigDecimal(),
            prosent = 9.6.toBigDecimal(),
            antallPersoner = 1000,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.BRANSJE,
            kode = Bransje.SYKEHJEM.navn,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
            tapteDagsverk = 311214.1.toBigDecimal(),
            muligeDagsverk = 3782127.8.toBigDecimal(),
            prosent = 8.2.toBigDecimal(),
            antallPersoner = 1000,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.BRANSJE,
            kode = Bransje.SYKEHJEM.navn,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
            tapteDagsverk = 327662.8.toBigDecimal(),
            muligeDagsverk = 3511634.6.toBigDecimal(),
            prosent = 9.3.toBigDecimal(),
            antallPersoner = 1000,
        )
    }

    fun lagSektorStatistikkTestCase() {
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.SEKTOR,
            kode = "3",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
            tapteDagsverk = 5300255.308034.toBigDecimal(),
            muligeDagsverk = 94813876.585998.toBigDecimal(),
            antallPersoner = 2047614,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.SEKTOR,
            kode = "3",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
            tapteDagsverk = 4814260.178576.toBigDecimal(),
            muligeDagsverk = 92474907.299356.toBigDecimal(),
            antallPersoner = 2092081,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.SEKTOR,
            kode = "3",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
            tapteDagsverk = 4905415.411417.toBigDecimal(),
            muligeDagsverk = 103456981.055736.toBigDecimal(),
            antallPersoner = 2174010,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.SEKTOR,
            kode = "3",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
            tapteDagsverk = 5276690.149223.toBigDecimal(),
            muligeDagsverk = 99698415.989531.toBigDecimal(),
            antallPersoner = 2106935,
        )
    }

    fun lagLandStatistikkTestCase() {
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
            tapteDagsverk = 8894430.toBigDecimal(),
            muligeDagsverk = 142947000.toBigDecimal(),
            antallPersoner = 3124427,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
            tapteDagsverk = 8152210.toBigDecimal(),
            muligeDagsverk = 139269000.toBigDecimal(),
            antallPersoner = 3166683,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
            tapteDagsverk = 7988370.toBigDecimal(),
            muligeDagsverk = 155487000.toBigDecimal(),
            antallPersoner = 3303089,
        )
        sendSykefraværsstatistikk(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
            tapteDagsverk = 8774880.toBigDecimal(),
            muligeDagsverk = 150138000.toBigDecimal(),
            antallPersoner = 3190634,
        )
    }

    fun sendSykefraværsstatistikk(
        årstallOgKvartal: ÅrstallOgKvartal,
        kategori: Statistikkategori,
        kode: String,
        tapteDagsverk: BigDecimal,
        muligeDagsverk: BigDecimal,
        tapteDagsverGradert: BigDecimal = 0.0.toBigDecimal(),
        tapteDagsverkMedVarighet: List<TapteDagsverkPerVarighet> = emptyList(),
        antallPersoner: Int,
    ) {
        sendSykefraværsstatistikk(
            årstallOgKvartal = årstallOgKvartal,
            kategori = kategori,
            kode = kode,
            tapteDagsverk = tapteDagsverk,
            muligeDagsverk = muligeDagsverk,
            tapteDagsverGradert = tapteDagsverGradert,
            tapteDagsverkMedVarighet = tapteDagsverkMedVarighet,
            prosent = sykefraværsprosent(tapteDagsverk = tapteDagsverk, muligeDagsverk = muligeDagsverk).toBigDecimal(),
            antallPersoner = antallPersoner,
        )
    }

    fun sendSykefraværsstatistikk(
        årstallOgKvartal: ÅrstallOgKvartal,
        kategori: Statistikkategori,
        kode: String,
        tapteDagsverk: BigDecimal,
        muligeDagsverk: BigDecimal,
        prosent: BigDecimal,
        tapteDagsverGradert: BigDecimal = 0.0.toBigDecimal(),
        tapteDagsverkMedVarighet: List<TapteDagsverkPerVarighet> = emptyList(),
        antallPersoner: Int,
    ) {
        val jsonMelding = JsonMelding(
            kategori = kategori,
            kode = kode,
            årstallOgKvartal = årstallOgKvartal,
            tapteDagsverk = tapteDagsverk, // TODO: JsonMelding skal være BigDecimal
            muligeDagsverk = muligeDagsverk,
            prosent = prosent,
            tapteDagsverGradert = tapteDagsverGradert,
            tapteDagsverkMedVarighet = tapteDagsverkMedVarighet,
            antallPersoner = antallPersoner,
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            nøkkel = jsonMelding.toJsonKey(),
            melding = jsonMelding.toJsonValue(),
            topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )
    }

    private fun sykefraværsprosent(
        tapteDagsverk: BigDecimal,
        muligeDagsverk: BigDecimal,
    ): Double =
        tapteDagsverk.divide(muligeDagsverk, 3, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(1, RoundingMode.HALF_UP).toDouble()
}
