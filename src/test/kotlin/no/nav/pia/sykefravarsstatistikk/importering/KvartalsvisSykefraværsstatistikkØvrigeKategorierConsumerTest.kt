package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkGjeldendeKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkMedVarighet
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import kotlin.test.Test

class KvartalsvisSykefraværsstatistikkØvrigeKategorierConsumerTest {
    @Test
    fun `sykefraværsstatistikk for kategori LAND lagres i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = KVARTAL_2024_3,
            tapteDagsverk = 17.5.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            prosent = 2.3.toBigDecimal(),
            antallPersoner = 4,
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        val statistikkQ12023 = hentStatistikkGjeldendeKvartal(
            kategori = Statistikkategori.LAND,
            verdi = "NO",
            kvartal = KVARTAL_2024_3,
            tabellnavn = "sykefravarsstatistikk_land",
            kodenavn = "land",
        )

        statistikkQ12023.tapteDagsverk bigDecimalShouldBe 17.5
        statistikkQ12023.muligeDagsverk bigDecimalShouldBe 761.3
        statistikkQ12023.prosent bigDecimalShouldBe 2.3
        statistikkQ12023.antallPersoner shouldBe 4
    }

    @Test
    fun `sykefraværsstatistikk for kategori SEKTOR lagres i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.SEKTOR,
            kode = "1",
            årstallOgKvartal = KVARTAL_2024_3,
            tapteDagsverk = 17.5.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            prosent = 2.3.toBigDecimal(),
            antallPersoner = 4,
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        val statistikkQ32024 = hentStatistikkGjeldendeKvartal(
            kategori = Statistikkategori.SEKTOR,
            verdi = "1",
            kvartal = KVARTAL_2024_3,
            tabellnavn = "sykefravarsstatistikk_sektor",
            kodenavn = "sektor",
        )

        statistikkQ32024.kategori shouldBe Statistikkategori.SEKTOR
        statistikkQ32024.kode shouldBe "1"
        statistikkQ32024.tapteDagsverk bigDecimalShouldBe 17.5
        statistikkQ32024.muligeDagsverk bigDecimalShouldBe 761.3
        statistikkQ32024.prosent bigDecimalShouldBe 2.3
        statistikkQ32024.antallPersoner shouldBe 4
    }

    @Test
    fun `sykefraværsstatistikk for kategori NÆRING lagres i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.NÆRING,
            kode = "22",
            årstallOgKvartal = KVARTAL_2024_3,
            prosent = 2.7.toBigDecimal(),
            tapteDagsverk = 5039.8.toBigDecimal(),
            muligeDagsverk = 186.3.toBigDecimal(),
            antallPersoner = 3,
            tapteDagsverGradert = 0.0.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2.toBigDecimal(),
                ),
            ),
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        val statistikkQ32024 = hentStatistikkGjeldendeKvartal(
            kategori = Statistikkategori.NÆRING,
            verdi = "22",
            kvartal = KVARTAL_2024_3,
            tabellnavn = "sykefravarsstatistikk_naring",
            kodenavn = "naring",
        )
        statistikkQ32024.kategori shouldBe Statistikkategori.NÆRING
        statistikkQ32024.kode shouldBe "22"
        statistikkQ32024.tapteDagsverk bigDecimalShouldBe 5039.8

        val statistikkMedVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_naring_med_varighet",
            kolonnenavn = "naring",
            verdi = "22",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        statistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "A" }.tapteDagsverk bigDecimalShouldBe 12.3
        statistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "D" }.tapteDagsverk bigDecimalShouldBe 5.2
    }

    @Test
    fun `sykefraværsstatistikk for kategori NÆRINGSKODE lagres i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.NÆRINGSKODE,
            kode = "88911",
            årstallOgKvartal = KVARTAL_2024_3,
            tapteDagsverk = 17.5.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            prosent = 2.3.toBigDecimal(),
            antallPersoner = 4,
            tapteDagsverGradert = 19.2.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2.toBigDecimal(),
                ),
            ),
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        val statistikkQ32024 = hentStatistikkGjeldendeKvartal(
            kategori = Statistikkategori.NÆRINGSKODE,
            verdi = "88911",
            kvartal = KVARTAL_2024_3,
            tabellnavn = "sykefravarsstatistikk_naringskode",
            kodenavn = "naringskode",
        )

        statistikkQ32024.kategori shouldBe Statistikkategori.NÆRINGSKODE
        statistikkQ32024.kode shouldBe "88911"
        statistikkQ32024.tapteDagsverk bigDecimalShouldBe 17.5
        statistikkQ32024.muligeDagsverk bigDecimalShouldBe 761.3
        statistikkQ32024.prosent bigDecimalShouldBe 2.3
        statistikkQ32024.antallPersoner shouldBe 4

        val næringskodeStatistikkMedVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_naringskode_med_varighet",
            kolonnenavn = "naringskode",
            verdi = "88911",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        næringskodeStatistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "A" }.tapteDagsverk bigDecimalShouldBe 12.3
        næringskodeStatistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "D" }.tapteDagsverk bigDecimalShouldBe 5.2
    }

    @Test
    fun `sykefraværsstatistikk for kategori BRANSJE lagres i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.BRANSJE,
            kode = "22",
            årstallOgKvartal = KVARTAL_2024_3,
            prosent = 2.7.toBigDecimal(),
            tapteDagsverk = 5039.8.toBigDecimal(),
            muligeDagsverk = 186.3.toBigDecimal(),
            antallPersoner = 3,
            tapteDagsverGradert = 0.0.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2.toBigDecimal(),
                ),
            ),
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        val statistikkQ32024 = hentStatistikkGjeldendeKvartal(
            kategori = Statistikkategori.BRANSJE,
            verdi = "22",
            kvartal = KVARTAL_2024_3,
            tabellnavn = "sykefravarsstatistikk_bransje",
            kodenavn = "bransje",
        )

        statistikkQ32024.kategori shouldBe Statistikkategori.BRANSJE
        statistikkQ32024.kode shouldBe "22"
        statistikkQ32024.tapteDagsverk bigDecimalShouldBe 5039.8
        statistikkQ32024.muligeDagsverk bigDecimalShouldBe 186.3
        statistikkQ32024.prosent bigDecimalShouldBe 2.7
        statistikkQ32024.antallPersoner shouldBe 3

        val bransjeStatistikkMedVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_bransje_med_varighet",
            kolonnenavn = "bransje",
            verdi = "22",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        bransjeStatistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "A" }.tapteDagsverk bigDecimalShouldBe 12.3
        bransjeStatistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "D" }.tapteDagsverk bigDecimalShouldBe 5.2
    }
}
