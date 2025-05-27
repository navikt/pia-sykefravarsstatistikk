package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeAnlegg
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.Ignore

class StatistikkEksportVirksomhetTest {
    companion object {
        private val importKategori = Statistikkategori.VIRKSOMHET
        private val importTopic = Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER

        private val eksportKategori = Statistikkategori.VIRKSOMHET
        private val eksportTopic = Topic.STATISTIKK_EKSPORT_VIRKSOMHET
        private val eksportKonsument = kafkaContainerHelper.nyKonsument(topic = eksportTopic)

        private val KVARTAL_2025_1 = ÅrstallOgKvartal(2025, 1)

        @BeforeClass
        @JvmStatic
        fun setUp() {
            eksportKonsument.subscribe(mutableListOf(eksportTopic.navn))
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            eksportKonsument.unsubscribe()
            eksportKonsument.close()
        }
    }

    @Ignore
    fun `sykefraværsstatistikk for kategori VIRKSOMHET blir eksportert til kafka`() {
        val virksomhet: Virksomhet = underenhetIBransjeAnlegg.somNæringsdrivende()

        kafkaContainerHelper.sendVirksomhetsstatistikk(virksomhet = virksomhet)

        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = virksomhet.orgnr,
            årstallOgKvartal = KVARTAL_2025_1,
            prosent = 28.3.toBigDecimal(),
            tapteDagsverk = 154.5439.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            antallPersoner = 4,
            tapteDagsverGradert = 33.2.toBigDecimal(),
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

        val eksportNøkkel: String =
            ObjectMapper().writeValueAsString(
                mapOf(
                    "kategori" to eksportKategori,
                    // TODO kan være en annen enn importkategori, da virksomhet blir til to forskjellige kategorier?
                    "kode" to virksomhet.orgnr,
                    "kvartal" to KVARTAL_2025_1.kvartal.toString(),
                    "årstall" to KVARTAL_2025_1.årstall.toString(),
                ),
            )

        runBlocking {
            kafkaContainerHelper.sendOgKonsumerFraAnnetTopic(
                importMelding = sykefraværsstatistikk,
                importTopic = importTopic,
                eksportNøkkel = eksportNøkkel,
                eksportKonsument = eksportKonsument,
            ) { meldinger ->
                val objektene = meldinger.map { Json.decodeFromString<SykefraværsstatistikkPerKategoriEksportDto>(it) }
                objektene shouldHaveAtLeastSize 1
                objektene.forEach {
                    it.kategori shouldBe eksportKategori
                    it.kode shouldBe virksomhet.orgnr
                }
            }
        }

        applikasjon.shouldContainLog(
            "Eksporterer sykefraværsstatistikk for $importKategori - 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Eksporterer sykefraværsstatistikk for $eksportKategori - 1. kvartal 2025".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori $importKategori, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
        applikasjon.shouldContainLog(
            "Melding eksportert på Kafka for statistikkategori $eksportKategori, 4 kvartaler fram til 1. kvartal 2025.".toRegex(),
        )
    }
}
