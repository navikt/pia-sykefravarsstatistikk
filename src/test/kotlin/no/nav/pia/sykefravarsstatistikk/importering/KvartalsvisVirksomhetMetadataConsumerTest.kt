package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.VirksomhetMetadataImportTestUtils.Companion.hentVirksomhetMetadataStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.VirksomhetMetadataImportTestUtils.VirksomhetMetadataJsonMelding
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import kotlin.test.Test

class KvartalsvisVirksomhetMetadataConsumerTest {
    @Test
    fun `Kafkamelding om metadata for VIRKSOMHET_METADATA blir lagret i DB`() {
        val virksomhetMetadataStatistikk = VirksomhetMetadataJsonMelding(
            orgnr = "998877665",
            årstallOgKvartal = KVARTAL_2024_3,
            sektor = "2",
            primærnæring = "88",
            primærnæringskode = "88911",
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
        metadataQ32024.primærnæring shouldBe "88"
        metadataQ32024.primærnæringskode shouldBe "88911"
    }

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
