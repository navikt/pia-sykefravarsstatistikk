package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import kotlin.test.Test

class EksportTest {
    @Test
    fun `sykefraværsstatistikk for kategori LAND blir eksportert til kafka`() {
        kafkaContainerHelper.sendLandsstatistikk()

        val kvartal20251 = ÅrstallOgKvartal(2025, 1)

        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = kvartal20251,
            tapteDagsverk = 17.5.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            prosent = 2.3.toBigDecimal(),
            antallPersoner = 4,
        )

        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        applikasjon.shouldContainLog(
            "Eksporterer sykefraværsstatistikk for LAND fra 2. kvartal 2024 til 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori LAND, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
    }
}
