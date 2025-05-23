package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkMedVarighet
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentVirksomhetStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import kotlin.test.Test

class SykefraværstatistikkConsumerTest {
    @Test
    fun `Melding om sykefraværsstatistikk i Kafka for virksomhet blir lagret i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
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
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        )

        val statistikkQ12023 = hentVirksomhetStatistikk(
            orgnr = "987654321",
            kvartal = KVARTAL_2024_3,
        )
        statistikkQ12023.orgnr shouldBe "987654321"
        statistikkQ12023.tapteDagsverk bigDecimalShouldBe 154.5439
        statistikkQ12023.muligeDagsverk bigDecimalShouldBe 761.3
        statistikkQ12023.prosent bigDecimalShouldBe 28.3
        statistikkQ12023.antallPersoner shouldBe 4
        statistikkQ12023.tapteDagsverkGradertSykemelding bigDecimalShouldBe 33.2

        val statistikkMedVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_virksomhet_med_varighet",
            kolonnenavn = "orgnr",
            verdi = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        statistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "A" }.tapteDagsverk bigDecimalShouldBe 12.3
        statistikkMedVarighet.tapteDagsverkMedVarighet.first { it.varighet == "D" }.tapteDagsverk bigDecimalShouldBe 5.2
    }

    @Test
    fun `Oppdaterer liste med nye verdier for tapte dagsverk per varighet`() {
        val gammelTapteDagsverkPerVarighet = listOf(
            TapteDagsverkPerVarighet(
                varighet = "A",
                tapteDagsverk = 12.3.toBigDecimal(),
            ),
            TapteDagsverkPerVarighet(
                varighet = "D",
                tapteDagsverk = 5.2.toBigDecimal(),
            ),
        )

        var sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
            prosent = 28.3.toBigDecimal(),
            tapteDagsverk = 154.5439.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            antallPersoner = 4,
            tapteDagsverGradert = 33.2.toBigDecimal(),
            tapteDagsverkMedVarighet = gammelTapteDagsverkPerVarighet,
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        )

        val statistikkQ32024 = hentVirksomhetStatistikk(
            orgnr = "987654321",
            kvartal = KVARTAL_2024_3,
        )
        statistikkQ32024.orgnr shouldBe "987654321"
        statistikkQ32024.tapteDagsverk bigDecimalShouldBe 154.5439
        statistikkQ32024.muligeDagsverk bigDecimalShouldBe 761.3
        statistikkQ32024.prosent bigDecimalShouldBe 28.3
        statistikkQ32024.antallPersoner shouldBe 4
        statistikkQ32024.tapteDagsverkGradertSykemelding bigDecimalShouldBe 33.2

        val statistikkMedGammelVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_virksomhet_med_varighet",
            kolonnenavn = "orgnr",
            verdi = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        statistikkMedGammelVarighet.tapteDagsverkMedVarighet.first { it.varighet == "A" }.tapteDagsverk bigDecimalShouldBe 12.3
        statistikkMedGammelVarighet.tapteDagsverkMedVarighet.first { it.varighet == "D" }.tapteDagsverk bigDecimalShouldBe 5.2

        val nyTapteDagsverkPerVarighet = listOf(
            TapteDagsverkPerVarighet(
                varighet = "A",
                tapteDagsverk = 12.3.toBigDecimal(),
            ),
            TapteDagsverkPerVarighet(
                varighet = "C",
                tapteDagsverk = 4.4.toBigDecimal(),
            ),
        )

        sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
            prosent = 28.3.toBigDecimal(),
            tapteDagsverk = 154.5439.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            antallPersoner = 4,
            tapteDagsverGradert = 33.2.toBigDecimal(),
            tapteDagsverkMedVarighet = nyTapteDagsverkPerVarighet,
        )

        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        )

        val nyStatistikkQ32024 = hentVirksomhetStatistikk(
            orgnr = "987654321",
            kvartal = KVARTAL_2024_3,
        )
        nyStatistikkQ32024.orgnr shouldBe "987654321"

        val statistikkMedNyVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_virksomhet_med_varighet",
            kolonnenavn = "orgnr",
            verdi = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        statistikkMedNyVarighet.tapteDagsverkMedVarighet.first { it.varighet == "A" }.tapteDagsverk bigDecimalShouldBe 12.3
        statistikkMedNyVarighet.tapteDagsverkMedVarighet.first { it.varighet == "C" }.tapteDagsverk bigDecimalShouldBe 4.4
    }
}
