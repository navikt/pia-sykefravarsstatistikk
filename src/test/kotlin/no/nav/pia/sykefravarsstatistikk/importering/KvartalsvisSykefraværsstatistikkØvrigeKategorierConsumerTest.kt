package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentNæringStatistikkMedVarighet
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkGjeldendeKvartal
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
            tapteDagsverk = 17.5,
            muligeDagsverk = 761.3,
            prosent = 2.3,
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
            tapteDagsverk = 17.5,
            muligeDagsverk = 761.3,
            prosent = 2.3,
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
            prosent = 2.7,
            tapteDagsverk = 5039.8,
            muligeDagsverk = 186.3,
            antallPersoner = 3,
            tapteDagsverGradert = 0.0,
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2,
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

        val virksomhetStatistikkMedVarighet = hentNæringStatistikkMedVarighet(
            næring = "22",
            kvartal = KVARTAL_2024_3,
        )

        virksomhetStatistikkMedVarighet.næring shouldBe "22"
        virksomhetStatistikkMedVarighet.tapteDagsverkMedVarighet shouldBe listOf(
            TapteDagsverkPerVarighet(
                varighet = "A",
                tapteDagsverk = 12.3,
            ),
            TapteDagsverkPerVarighet(
                varighet = "D",
                tapteDagsverk = 5.2,
            ),
        )
    }
}
