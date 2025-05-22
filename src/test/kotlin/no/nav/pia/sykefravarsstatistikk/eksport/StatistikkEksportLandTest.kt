package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.Test

class StatistikkEksportLandTest {
    companion object {
        private val kategori = Statistikkategori.LAND
        private val eksportTopic = Topic.STATISTIKK_EKSPORT_LAND
        private val importTopic = Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER
        private val konsument = kafkaContainerHelper.nyKonsument(topic = eksportTopic)

        private val kvartal20251 = ÅrstallOgKvartal(2025, 1)

        @BeforeClass
        @JvmStatic
        fun setUp() = konsument.subscribe(mutableListOf(eksportTopic.navn))

        @AfterClass
        @JvmStatic
        fun tearDown() {
            konsument.unsubscribe()
            konsument.close()
        }
    }

    @Test
    fun `sykefraværsstatistikk for kategori LAND blir eksportert til kafka`() {
        kafkaContainerHelper.sendLandsstatistikk(startÅr = 2024, sluttÅr = 2024)
        val importKode = "NO"
        val eksportKode = "Norge"

        val sykefraværsstatistikk = JsonMelding(
            kategori = kategori,
            kode = importKode,
            årstallOgKvartal = kvartal20251,
            tapteDagsverk = 17.5.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            prosent = 2.3.toBigDecimal(),
            antallPersoner = 4,
        )
        val nøkkel: String = ObjectMapper().writeValueAsString(
            mapOf(
                "kategori" to kategori,
                "kode" to eksportKode,
                "kvartal" to kvartal20251.kvartal.toString(),
                "årstall" to kvartal20251.årstall.toString(),
            ),
        )

        runBlocking {
            kafkaContainerHelper.sendOgKonsumerFraAnnetTopic(
                importMelding = sykefraværsstatistikk,
                importTopic = importTopic,
                eksportNøkkel = nøkkel,
                eksportKonsument = konsument,
            ) { meldinger ->
                val objektene = meldinger.map { Json.decodeFromString<SykefraværsstatistikkPerKategoriEksportDto>(it) }
                objektene shouldHaveAtLeastSize 1
                objektene.forEach {
                    it.kategori shouldBe kategori
                    it.kode shouldBe eksportKode
                }
            }
        }

        applikasjon.shouldContainLog(
            "Eksporterer sykefraværsstatistikk for $kategori - 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori $kategori, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
    }
}
