package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkGjeldendeKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import kotlin.test.Test

class KvartalsvisSykefraværsstatistikkØvrigeKategorierConsumerTest {
    @Test
    fun `sykefraværsstatistikk lagres i DB`() {
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
}
