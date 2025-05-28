package no.nav.pia.sykefravarsstatistikk.importering

import ia.felles.definisjoner.bransjer.Bransje.BARNEHAGER
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
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
        val orgnr = "998877665"
        val sektor = Sektor.PRIVAT
        val næring = "88"
        val primærnæringskode = "88911"
        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = orgnr,
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = sektor.kode,
            primærnæring = næring,
            primærnæringskode = primærnæringskode,
        )
        kafkaContainerHelper.sendKafkaMelding(
            virksomhetMetadataStatistikk.toJsonKey(),
            virksomhetMetadataStatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        )

        runBlocking {
            kafkaContainerHelper.ventOgKonsumerKafkaMeldinger(
                key = Json.encodeToString(
                    VirksomhetMetadataNøkkel(orgnr = orgnr, årstall = KVARTAL_2024_3.årstall, kvartal = KVARTAL_2024_3.kvartal),
                ),
                konsument = eksportKonsument,
                block = { meldinger ->
                    val objektene = meldinger.map { Json.decodeFromString<VirksomhetMetadataKafkamelding>(it) }
                    objektene shouldHaveAtLeastSize 1
                    objektene.forEach {
                        it.orgnr shouldBe orgnr
                        it.årstall shouldBe KVARTAL_2024_3.årstall
                        it.kvartal shouldBe KVARTAL_2024_3.kvartal
                        it.næring shouldBe næring
                        it.næringskode shouldBe primærnæringskode
                        it.bransje shouldBe BARNEHAGER.name
                        it.sektor shouldBe sektor.name
                    }
                },
            )
        }

        val metadataQ32024 = hentVirksomhetMetadataStatistikk(
            orgnr = orgnr,
            kvartal = KVARTAL_2024_3,
        )
        metadataQ32024.orgnr shouldBe orgnr
        metadataQ32024.årstall shouldBe 2024
        metadataQ32024.kvartal shouldBe 3
        metadataQ32024.sektor shouldBe "3"
        metadataQ32024.primærnæring shouldBe næring
        metadataQ32024.primærnæringskode shouldBe primærnæringskode
    }

    @Test
    fun `Kafkamelding om metadata for VIRKSOMHET_METADATA eksporteres uten Bransje dersom virksomhet ikke hører til noen bransjer`() {
        val orgnr = "998377568"
        val primærnæring = "88"
        val primærnæringskode = "88001"
        val sektor = Sektor.KOMMUNAL
        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = orgnr,
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = sektor.kode,
            primærnæring = primærnæring,
            primærnæringskode = primærnæringskode,
        )
        kafkaContainerHelper.sendKafkaMelding(
            virksomhetMetadataStatistikk.toJsonKey(),
            virksomhetMetadataStatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        )

        runBlocking {
            kafkaContainerHelper.ventOgKonsumerKafkaMeldinger(
                key = Json.encodeToString(
                    VirksomhetMetadataNøkkel(orgnr = orgnr, årstall = KVARTAL_2024_3.årstall, kvartal = KVARTAL_2024_3.kvartal),
                ),
                konsument = eksportKonsument,
                block = { meldinger ->
                    val objektene = meldinger.map { Json.decodeFromString<VirksomhetMetadataKafkamelding>(it) }
                    objektene shouldHaveAtLeastSize 1
                    objektene.forEach {
                        it.orgnr shouldBe orgnr
                        it.årstall shouldBe KVARTAL_2024_3.årstall
                        it.kvartal shouldBe KVARTAL_2024_3.kvartal
                        it.næring shouldBe primærnæring
                        it.næringskode shouldBe primærnæringskode
                        it.bransje shouldBe null
                        it.sektor shouldBe sektor.name
                    }
                },
            )
        }
    }

    @Test
    fun `Kafkamelding om metadata for VIRKSOMHET_METADATA kan eksporteres uten næring eller næringskode`() {
        val orgnr = "991137665"
        val sektor = Sektor.STATLIG

        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = orgnr,
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = sektor.kode,
            primærnæring = null,
            primærnæringskode = null,
        )
        kafkaContainerHelper.sendKafkaMelding(
            virksomhetMetadataStatistikk.toJsonKey(),
            virksomhetMetadataStatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        )

        runBlocking {
            kafkaContainerHelper.ventOgKonsumerKafkaMeldinger(
                key = Json.encodeToString(
                    VirksomhetMetadataNøkkel(orgnr = orgnr, årstall = KVARTAL_2024_3.årstall, kvartal = KVARTAL_2024_3.kvartal),
                ),
                konsument = eksportKonsument,
                block = { meldinger ->
                    val objektene = meldinger.map { Json.decodeFromString<VirksomhetMetadataKafkamelding>(it) }
                    objektene shouldHaveAtLeastSize 1
                    objektene.forEach {
                        it.orgnr shouldBe orgnr
                        it.årstall shouldBe KVARTAL_2024_3.årstall
                        it.kvartal shouldBe KVARTAL_2024_3.kvartal
                        it.sektor shouldBe sektor.name
                        it.næring shouldBe ""
                        it.næringskode shouldBe ""
                        it.bransje shouldBe null
                    }
                },
            )
        }
    }

    @Test
    fun `metadata for VIRKSOMHET_METADATA kan inneholde null-verdier for primærnæring og -primærnæringskode`() {
        val orgnr = "998877665"
        val sektor = Sektor.PRIVAT

        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = orgnr,
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = sektor.kode,
            primærnæring = null,
            primærnæringskode = null,
        )

        kafkaContainerHelper.sendOgVentTilKonsumert(
            virksomhetMetadataStatistikk.toJsonKey(),
            virksomhetMetadataStatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        )

        val virksomhetMetadataFraDb = hentVirksomhetMetadataStatistikk(
            orgnr = orgnr,
            kvartal = KVARTAL_2024_3,
        )
        virksomhetMetadataFraDb.orgnr shouldBe orgnr
        virksomhetMetadataFraDb.årstall shouldBe KVARTAL_2024_3.årstall
        virksomhetMetadataFraDb.kvartal shouldBe KVARTAL_2024_3.kvartal
        virksomhetMetadataFraDb.sektor shouldBe sektor.kode
        virksomhetMetadataFraDb.primærnæring shouldBe null
        virksomhetMetadataFraDb.primærnæringskode shouldBe null
    }
}
