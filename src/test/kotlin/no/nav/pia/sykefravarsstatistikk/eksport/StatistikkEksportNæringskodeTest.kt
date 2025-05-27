package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilNæringskode
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.Ignore

class StatistikkEksportNæringskodeTest {
    companion object {
        private val kategori = Statistikkategori.NÆRINGSKODE
        private val eksportTopic = Topic.STATISTIKK_EKSPORT_NÆRINGSKODE
        private val importTopic = Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER
        private val konsument = kafkaContainerHelper.nyKonsument(topic = eksportTopic)

        private val KVARTAL_2025_1 = ÅrstallOgKvartal(2025, 1)

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

    @Ignore
    fun `sykefraværsstatistikk for kategori NÆRINGSKODE blir eksportert til kafka`() {
        val næringskode: Næringskode = "88.911".tilNæringskode()
        kafkaContainerHelper.sendNæringskodestatistikk(næringskode = næringskode)

        val importKode = næringskode.femsifferIdentifikator
        val eksportKode = næringskode.femsifferIdentifikator

        val sykefraværsstatistikk = JsonMelding(
            kategori = kategori,
            kode = importKode,
            årstallOgKvartal = KVARTAL_2025_1,
            prosent = 5.8.toBigDecimal(),
            tapteDagsverk = 893.631879.toBigDecimal(),
            muligeDagsverk = 15407.446182.toBigDecimal(),
            antallPersoner = 299,
            tapteDagsverGradert = 365.466504.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 278.26.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 274.31.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "C",
                    tapteDagsverk = 87.62.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 31.21.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 113.4.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 108.83.toBigDecimal(),
                ),
            ),
        )

        val nøkkel: String = ObjectMapper().writeValueAsString(
            mapOf(
                "kategori" to kategori,
                "kode" to eksportKode,
                "kvartal" to KVARTAL_2025_1.kvartal.toString(),
                "årstall" to KVARTAL_2025_1.årstall.toString(),
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
