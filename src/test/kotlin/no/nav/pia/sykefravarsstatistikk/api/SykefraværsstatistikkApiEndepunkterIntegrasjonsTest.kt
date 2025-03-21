package no.nav.pia.sykefravarsstatistikk.api

import ia.felles.definisjoner.bransjer.Bransje
import io.kotest.inspectors.shouldForNone
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværsprosentDto
import no.nav.pia.sykefravarsstatistikk.api.dto.StatistikkJson
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetSykehjemUtenTilgang
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
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
            postgresContainerHelper.slettAlleStatistikk()
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
    fun `Sjekk format og data fra aggregert statistikk`() {
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

            val aggregertStatistikk = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhet.orgnr,
                config = withToken(),
            )

            aggregertStatistikk.alleTyperAggregerteStatistikk().forEach { aggregertStatistikkDto ->
                aggregertStatistikkDto.shouldForNone { statistikk ->
                    statistikk.statistikkategori shouldBe
                        Statistikkategori.OVERORDNET_ENHET.name
                }
            }

            aggregertStatistikk.prosentSiste4KvartalerTotalt.shouldHaveStatistikkForKategori(
                statistikkategori = Statistikkategori.LAND,
                label = "Norge",
            )

            listOf(
                aggregertStatistikk.prosentSiste4KvartalerTotalt,
                // TODO: vi har ikke gradert for bransje enda
                // aggregertStatistikk.prosentSiste4KvartalerGradert,
                aggregertStatistikk.prosentSiste4KvartalerKorttid,
                aggregertStatistikk.prosentSiste4KvartalerLangtid,
                aggregertStatistikk.trendTotalt,
            ).onEach { aggregertStatistikkDto ->
                aggregertStatistikkDto.shouldHaveStatistikkForKategori(
                    statistikkategori = Statistikkategori.BRANSJE,
                    label = Bransje.SYKEHJEM.navn,
                )
            }

            listOf(
                aggregertStatistikk.prosentSiste4KvartalerTotalt,
                aggregertStatistikk.prosentSiste4KvartalerGradert,
                aggregertStatistikk.prosentSiste4KvartalerKorttid,
                aggregertStatistikk.prosentSiste4KvartalerLangtid,
                aggregertStatistikk.tapteDagsverkTotalt,
                aggregertStatistikk.muligeDagsverkTotalt,
            ).onEach { aggregertStatistikkDto ->
                aggregertStatistikkDto.shouldHaveStatistikkForKategori(
                    statistikkategori = Statistikkategori.VIRKSOMHET,
                    label = underenhetSykehjemMedTilgang.navn,
                )
            }
        }
    }

    @Test
    fun `Sjekk format og data fra kvartalsvis statistikk`() {
        val underenhet: Underenhet = underenhetSykehjemMedTilgang

        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhet.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )
            val expectedStatistikkLand = lagLandStatistikkTestCase()
            val expectedStatistikkSektor = lagSektorStatistikkTestCase()
            val expectedStatistikkBransje = lagBransjeStatistikkTestCase()
            val expectedStatistikkVirksomhet = lagVirksomhetStatistikkTestCase()

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhet.orgnr,
                config = withToken(),
            )

            kvartalsvisStatistikk.shoudBeEqualForKategori(expectedStatistikkLand, Statistikkategori.LAND, "Norge")
            kvartalsvisStatistikk.shoudBeEqualForKategori(
                expectedStatistikkSektor,
                Statistikkategori.SEKTOR,
                Sektor.PRIVAT.beskrivelse,
            )
            kvartalsvisStatistikk.shoudBeEqualForKategori(
                expectedStatistikkBransje,
                Statistikkategori.BRANSJE,
                Bransje.SYKEHJEM.navn,
            )
            kvartalsvisStatistikk.shoudBeEqualForKategori(
                expectedStatistikkVirksomhet,
                Statistikkategori.VIRKSOMHET,
                underenhetSykehjemMedTilgang.navn,
            )

            kvartalsvisStatistikk.shoudBeEqualForKategori(
                expectedStatistikkForKategori = emptyList(),
                statistikkategori = Statistikkategori.OVERORDNET_ENHET,
                label = overordnetSykehjemUtenTilgang.navn,
            )
        }
    }

    private fun AggregertStatistikkResponseDto.alleTyperAggregerteStatistikk() =
        listOf(
            prosentSiste4KvartalerTotalt,
            prosentSiste4KvartalerGradert,
            prosentSiste4KvartalerKorttid,
            prosentSiste4KvartalerLangtid,
            trendTotalt,
            tapteDagsverkTotalt,
            muligeDagsverkTotalt,
        )

    private fun List<StatistikkJson>.shouldHaveStatistikkForKategori(
        statistikkategori: Statistikkategori,
        label: String,
    ) {
        this.shouldForOne { statistikk -> statistikk.statistikkategori shouldBe statistikkategori }
        val actualStatistikkForKategori =
            this.firstOrNull { it.statistikkategori == statistikkategori }
        actualStatistikkForKategori.shouldNotBeNull()
        actualStatistikkForKategori.label shouldBe label
    }

    private fun List<KvartalsvisSykefraværshistorikkDto>.shoudBeEqualForKategori(
        expectedStatistikkForKategori: List<KvartalsvisSykefraværsprosentDto>,
        statistikkategori: Statistikkategori,
        label: String,
    ) {
        this.shouldForOne { statistikk -> statistikk.type shouldBe statistikkategori.name }
        val actualStatistikkForKategori =
            this.firstOrNull { it.type == statistikkategori.name }
        actualStatistikkForKategori.shouldNotBeNull()
        actualStatistikkForKategori.label shouldBe label
        actualStatistikkForKategori.kvartalsvisSykefraværsprosent.shouldBeEqual(expectedStatistikkForKategori)
    }

    private fun List<KvartalsvisSykefraværsprosentDto>.shouldBeEqual(expectedStatistikk: List<KvartalsvisSykefraværsprosentDto>) {
        this.size shouldBe expectedStatistikk.size
        this.forEachIndexed { index, kvartalsvisSykefraværsprosentDto ->
            val expected = expectedStatistikk[index]
            kvartalsvisSykefraværsprosentDto.tapteDagsverk?.bigDecimalShouldBeEqual(expected.tapteDagsverk)
            kvartalsvisSykefraværsprosentDto.muligeDagsverk?.bigDecimalShouldBeEqual(expected.muligeDagsverk)
            kvartalsvisSykefraværsprosentDto.prosent?.bigDecimalShouldBeEqual(expected.prosent)
            kvartalsvisSykefraværsprosentDto.årstall shouldBeEqual expected.årstall
            kvartalsvisSykefraværsprosentDto.kvartal shouldBeEqual expected.kvartal
        }
    }

    private infix fun BigDecimal.bigDecimalShouldBeEqual(expected: BigDecimal?) {
        (this.compareTo(expected) == 0) shouldBe true
    }

    private fun lagVirksomhetStatistikkTestCase(): List<KvartalsvisSykefraværsprosentDto> {
        // Data er tatt fra sykefravar_statistikk_virksomhet_med_gradering (i dev)
        // tapteDv = sykefravar_statistikk_virksomhet_med_gradering.tapte_dagsverk
        //  men det kunne også har vært sum av tapteDV i tapteDagsverkMedVarighet lista
        // tapteDagsverGradert = = sykefravar_statistikk_virksomhet_med_gradering.tapte_dagsverk_gradert_sykemelding
        // antallPersoner = = sykefravar_statistikk_virksomhet_med_gradering.antall_personer
        val statistikk: MutableList<KvartalsvisSykefraværsprosentDto> = mutableListOf()
        statistikk.add(
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
            ),
        )
        statistikk.add(
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
            ),
        )
        statistikk.add(
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
            ),
        )
        statistikk.add(
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
            ),
        )
        return statistikk
    }

    private fun lagBransjeStatistikkTestCase(): List<KvartalsvisSykefraværsprosentDto> {
        val statistikk: MutableList<KvartalsvisSykefraværsprosentDto> = mutableListOf()
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.BRANSJE,
                kode = Bransje.SYKEHJEM.navn,
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2023, kvartal = 4),
                tapteDagsverk = 312364.7.toBigDecimal(),
                muligeDagsverk = 3123505.8.toBigDecimal(),
                prosent = 9.8.toBigDecimal(),
                tapteDagsverGradert = 21234.790000.toBigDecimal(),
                tapteDagsverkMedVarighet = listOf(
                    TapteDagsverkPerVarighet(
                        varighet = "A",
                        tapteDagsverk = 4123.340000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "D",
                        tapteDagsverk = 812.330000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "E",
                        tapteDagsverk = 171.230000.toBigDecimal(),
                    ),
                ),
                antallPersoner = 96123,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.BRANSJE,
                kode = Bransje.SYKEHJEM.navn,
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
                tapteDagsverk = 330864.7.toBigDecimal(),
                muligeDagsverk = 3331505.8.toBigDecimal(),
                prosent = 9.9.toBigDecimal(),
                tapteDagsverGradert = 28664.790000.toBigDecimal(),
                tapteDagsverkMedVarighet = listOf(
                    TapteDagsverkPerVarighet(
                        varighet = "A",
                        tapteDagsverk = 4314.340000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "D",
                        tapteDagsverk = 892.130000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "E",
                        tapteDagsverk = 171.020000.toBigDecimal(),
                    ),
                ),
                antallPersoner = 96403,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.BRANSJE,
                kode = Bransje.SYKEHJEM.navn,
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
                tapteDagsverk = 312606.4.toBigDecimal(),
                muligeDagsverk = 3245624.8.toBigDecimal(),
                prosent = 9.6.toBigDecimal(),
                tapteDagsverGradert = 18965.760000.toBigDecimal(),
                tapteDagsverkMedVarighet = listOf(
                    TapteDagsverkPerVarighet(
                        varighet = "B",
                        tapteDagsverk = 223.980000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "C",
                        tapteDagsverk = 1674.030000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "E",
                        tapteDagsverk = 812.030000.toBigDecimal(),
                    ),
                ),
                antallPersoner = 96711,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.BRANSJE,
                kode = Bransje.SYKEHJEM.navn,
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
                tapteDagsverk = 311214.1.toBigDecimal(),
                muligeDagsverk = 3782127.8.toBigDecimal(),
                prosent = 8.2.toBigDecimal(),
                tapteDagsverGradert = 23123.990000.toBigDecimal(),
                tapteDagsverkMedVarighet = listOf(
                    TapteDagsverkPerVarighet(
                        varighet = "C",
                        tapteDagsverk = 23.080000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "D",
                        tapteDagsverk = 2764.990000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "F",
                        tapteDagsverk = 312.430000.toBigDecimal(),
                    ),
                ),
                antallPersoner = 104737,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.BRANSJE,
                kode = Bransje.SYKEHJEM.navn,
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
                tapteDagsverk = 327662.8.toBigDecimal(),
                muligeDagsverk = 3511634.6.toBigDecimal(),
                prosent = 9.3.toBigDecimal(),
                tapteDagsverGradert = 46675.600000.toBigDecimal(),
                tapteDagsverkMedVarighet = listOf(
                    TapteDagsverkPerVarighet(
                        varighet = "A",
                        tapteDagsverk = 443.080000.toBigDecimal(),
                    ),
                    TapteDagsverkPerVarighet(
                        varighet = "F",
                        tapteDagsverk = 344.430000.toBigDecimal(),
                    ),
                ),
                antallPersoner = 96778,
            ),
        )
        return statistikk
    }

    private fun lagSektorStatistikkTestCase(): List<KvartalsvisSykefraværsprosentDto> {
        val statistikk: MutableList<KvartalsvisSykefraværsprosentDto> = mutableListOf()
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.SEKTOR,
                kode = "3",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
                tapteDagsverk = 5300255.308034.toBigDecimal(),
                muligeDagsverk = 94813876.585998.toBigDecimal(),
                antallPersoner = 2047614,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.SEKTOR,
                kode = "3",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
                tapteDagsverk = 4814260.178576.toBigDecimal(),
                muligeDagsverk = 92474907.299356.toBigDecimal(),
                antallPersoner = 2092081,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.SEKTOR,
                kode = "3",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
                tapteDagsverk = 4905415.411417.toBigDecimal(),
                muligeDagsverk = 103456981.055736.toBigDecimal(),
                antallPersoner = 2174010,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.SEKTOR,
                kode = "3",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
                tapteDagsverk = 5276690.149223.toBigDecimal(),
                muligeDagsverk = 99698415.989531.toBigDecimal(),
                antallPersoner = 2106935,
            ),
        )
        return statistikk
    }

    fun lagLandStatistikkTestCase(): List<KvartalsvisSykefraværsprosentDto> {
        val statistikk: MutableList<KvartalsvisSykefraværsprosentDto> = mutableListOf()
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.LAND,
                kode = "NO",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 1),
                tapteDagsverk = 8894430.000000.toBigDecimal(),
                muligeDagsverk = 142947000.000000.toBigDecimal(),
                antallPersoner = 3124427,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.LAND,
                kode = "NO",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 2),
                tapteDagsverk = 8152210.000000.toBigDecimal(),
                muligeDagsverk = 139269000.000000.toBigDecimal(),
                antallPersoner = 3166683,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.LAND,
                kode = "NO",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 3),
                tapteDagsverk = 7988370.000000.toBigDecimal(),
                muligeDagsverk = 155487000.000000.toBigDecimal(),
                antallPersoner = 3303089,
            ),
        )
        statistikk.add(
            sendSykefraværsstatistikk(
                kategori = Statistikkategori.LAND,
                kode = "NO",
                årstallOgKvartal = ÅrstallOgKvartal(årstall = 2024, kvartal = 4),
                tapteDagsverk = 8774880.000000.toBigDecimal(),
                muligeDagsverk = 150138000.000000.toBigDecimal(),
                antallPersoner = 3190634,
            ),
        )
        return statistikk
    }

    private fun sendSykefraværsstatistikk(
        årstallOgKvartal: ÅrstallOgKvartal,
        kategori: Statistikkategori,
        kode: String,
        tapteDagsverk: BigDecimal,
        muligeDagsverk: BigDecimal,
        tapteDagsverGradert: BigDecimal = 0.0.toBigDecimal(),
        tapteDagsverkMedVarighet: List<TapteDagsverkPerVarighet> = emptyList(),
        antallPersoner: Int,
    ): KvartalsvisSykefraværsprosentDto =
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
    ): KvartalsvisSykefraværsprosentDto {
        val dto = KvartalsvisSykefraværsprosentDto(
            årstall = årstallOgKvartal.årstall,
            kvartal = årstallOgKvartal.kvartal,
            tapteDagsverk = tapteDagsverk,
            muligeDagsverk = muligeDagsverk,
            prosent = prosent,
            erMaskert = antallPersoner < 4,
        )
        val jsonMelding = JsonMelding(
            kategori = kategori,
            kode = kode,
            årstallOgKvartal = årstallOgKvartal,
            tapteDagsverk = tapteDagsverk,
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
        return dto
    }

    private fun sykefraværsprosent(
        tapteDagsverk: BigDecimal,
        muligeDagsverk: BigDecimal,
    ): Double =
        tapteDagsverk.divide(muligeDagsverk, 3, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .setScale(1, RoundingMode.HALF_UP).toDouble()
}
