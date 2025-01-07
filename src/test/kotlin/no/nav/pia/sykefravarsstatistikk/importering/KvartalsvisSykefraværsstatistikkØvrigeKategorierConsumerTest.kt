package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkGjeldendeKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.shouldBeEqual
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

        statistikkQ12023.tapteDagsverk shouldBeEqual 17.5
        statistikkQ12023.muligeDagsverk shouldBeEqual 761.3
        statistikkQ12023.prosent shouldBeEqual 2.3
        statistikkQ12023.antallPersoner shouldBe 4
    }
}
