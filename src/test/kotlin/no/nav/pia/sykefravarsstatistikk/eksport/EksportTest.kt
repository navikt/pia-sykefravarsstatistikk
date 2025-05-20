package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtleieAvEiendom
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import kotlin.test.Test

class EksportTest {
    @Test
    fun `sykefraværsstatistikk for kategori LAND blir eksportert til kafka`() {
        kafkaContainerHelper.sendLandsstatistikk()
        val kategori = Statistikkategori.LAND

        val kvartal20251 = ÅrstallOgKvartal(2025, 1)

        val sykefraværsstatistikk = JsonMelding(
            kategori = kategori,
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
            "Eksporterer sykefraværsstatistikk for $kategori - 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori $kategori, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
    }

    @Test
    fun `sykefraværsstatistikk for kategori SEKTOR blir eksportert til kafka`() {
        kafkaContainerHelper.sendSektorstatistikk(Sektor.STATLIG)
        val kategori = Statistikkategori.SEKTOR

        val kvartal20251 = ÅrstallOgKvartal(2025, 1)

        val sykefraværsstatistikk = JsonMelding(
            kategori = kategori,
            kode = "1",
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
            "Eksporterer sykefraværsstatistikk for $kategori - 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori $kategori, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
    }

    @Test
    fun `sykefraværsstatistikk for kategori NÆRING blir eksportert til kafka`() {
        val næring: Næring = underenhetINæringUtleieAvEiendom.somNæringsdrivende().næringskode.næring
        val kategori = Statistikkategori.NÆRING
        kafkaContainerHelper.sendNæringsstatistikk(
            næring = næring,
        )

        val kvartal20251 = ÅrstallOgKvartal(2025, 1)

        val sykefraværsstatistikk = JsonMelding(
            kategori = kategori,
            kode = næring.tosifferIdentifikator,
            årstallOgKvartal = kvartal20251,
            prosent = 2.7.toBigDecimal(),
            tapteDagsverk = 5039.8.toBigDecimal(),
            muligeDagsverk = 186.3.toBigDecimal(),
            antallPersoner = 5,
            tapteDagsverGradert = 87.87601.toBigDecimal(),
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
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        )

        applikasjon.shouldContainLog(
            "Eksporterer sykefraværsstatistikk for $kategori - 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori $kategori, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
    }
}
