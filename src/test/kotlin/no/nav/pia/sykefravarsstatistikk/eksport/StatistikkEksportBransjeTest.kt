package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import ia.felles.definisjoner.bransjer.Bransje
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeAnlegg
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.Test

class StatistikkEksportBransjeTest {
    companion object {
        private val kategori = Statistikkategori.BRANSJE
        private val eksportTopic = Topic.STATISTIKK_EKSPORT_BRANSJE
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
    fun `sykefraværsstatistikk for kategori BRANSJE blir eksportert til kafka`() {
        val bransje: Bransje = underenhetIBransjeAnlegg.somNæringsdrivende().bransje()!!
        kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
        val importKode = bransje.navn
        val eksportKode = bransje.navn

        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.BRANSJE,
            kode = importKode,
            årstallOgKvartal = kvartal20251,
            prosent = 2.7.toBigDecimal(),
            tapteDagsverk = 5039.8.toBigDecimal(),
            muligeDagsverk = 186.3.toBigDecimal(),
            antallPersoner = 3,
            tapteDagsverGradert = 21.2345.toBigDecimal(),
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
    }
}
