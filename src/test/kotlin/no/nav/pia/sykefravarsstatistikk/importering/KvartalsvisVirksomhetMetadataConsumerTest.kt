package no.nav.pia.sykefravarsstatistikk.importering

import ia.felles.definisjoner.bransjer.Bransje.BARNEHAGER
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.eksport.VirksomhetMetadataKafkamelding
import no.nav.pia.sykefravarsstatistikk.eksport.VirksomhetMetadataNøkkel
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.VirksomhetMetadataImportTestUtils.Companion.hentVirksomhetMetadataStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.VirksomhetMetadataImportTestUtils.VirksomhetMetadataJsonMelding
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import org.junit.AfterClass
import org.junit.BeforeClass
import kotlin.test.Test

class KvartalsvisVirksomhetMetadataConsumerTest {
    companion object {
        private val eksportMetadataVirksomhetTopic = Topic.STATISTIKK_EKSPORT_METADATA_VIRKSOMHET
        private val eksportKonsument = kafkaContainerHelper.nyKonsument(topic = eksportMetadataVirksomhetTopic)

        @BeforeClass
        @JvmStatic
        fun setUp() = eksportKonsument.subscribe(mutableListOf(eksportMetadataVirksomhetTopic.navn))

        @AfterClass
        @JvmStatic
        fun tearDown() {
            eksportKonsument.unsubscribe()
            eksportKonsument.close()
        }
    }

    @Test
    fun `Kafkamelding om metadata for VIRKSOMHET_METADATA blir lagret i DB`() {
        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = "998877665",
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = "2",
            primærnæring = "88",
            primærnæringskode = "88911",
        )
        kafkaContainerHelper.sendKafkaMelding(
            virksomhetMetadataStatistikk.toJsonKey(),
            virksomhetMetadataStatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        )

        runBlocking {
            kafkaContainerHelper.ventOgKonsumerKafkaMeldinger(
                key = Json.encodeToString(VirksomhetMetadataNøkkel(orgnr = "998877665", årstall = 2024, kvartal = 3)),
                konsument = eksportKonsument,
                block = { meldinger ->
                    val objektene = meldinger.map { Json.decodeFromString<VirksomhetMetadataKafkamelding>(it) }
                    objektene shouldHaveAtLeastSize 1
                    objektene.forEach {
                        it.orgnr shouldBe "998877665"
                        it.årstall shouldBe 2024
                        it.kvartal shouldBe 3
                        it.næring shouldBe "88"
                        it.næringskode shouldBe "88911"
                        it.bransje shouldBe BARNEHAGER.name
                        it.sektor shouldBe "2"
                    }
                },
            )
        }

        val metadataQ32024 = hentVirksomhetMetadataStatistikk(
            orgnr = "998877665",
            kvartal = KVARTAL_2024_3,
        )
        metadataQ32024.orgnr shouldBe "998877665"
        metadataQ32024.årstall shouldBe 2024
        metadataQ32024.kvartal shouldBe 3
        metadataQ32024.sektor shouldBe "2"
        metadataQ32024.primærnæring shouldBe "88"
        metadataQ32024.primærnæringskode shouldBe "88911"
    }

    // TODO: test: skal takle nullverdier for primærnæring og primærnæringskode

    @Test
    fun `metadata for VIRKSOMHET_METADATA kan inneholde null-verdier for primærnæring og -primærnæringskode`() {
        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = "998877665",
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = "2",
            primærnæring = null,
            primærnæringskode = null,
        )

        kafkaContainerHelper.sendOgVentTilKonsumert(
            virksomhetMetadataStatistikk.toJsonKey(),
            virksomhetMetadataStatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        )

        val metadataQ32024 = hentVirksomhetMetadataStatistikk(
            orgnr = "998877665",
            kvartal = KVARTAL_2024_3,
        )
        metadataQ32024.orgnr shouldBe "998877665"
        metadataQ32024.årstall shouldBe 2024
        metadataQ32024.kvartal shouldBe 3
        metadataQ32024.sektor shouldBe "2"
        metadataQ32024.primærnæring shouldBe null
        metadataQ32024.primærnæringskode shouldBe null
    }
}
