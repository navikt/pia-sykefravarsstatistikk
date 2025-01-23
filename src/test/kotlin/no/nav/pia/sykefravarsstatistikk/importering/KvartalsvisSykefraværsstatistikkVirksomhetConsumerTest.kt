package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.KVARTAL_2024_3
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentVirksomhetStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.hentStatistikkMedVarighet
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import kotlin.test.Test

class KvartalsvisSykefraværsstatistikkVirksomhetConsumerTest {
    @Test
    fun `Melding om sykefraværsstatistikk i Kafka for virksomhet blir lagret i DB`() {
        val sykefraværsstatistikk = JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
            prosent = 2.3,
            tapteDagsverk = 17.5,
            muligeDagsverk = 761.3,
            antallPersoner = 4,
            tapteDagsverGradert = 33.2,
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2,
                ),
            ),
        )
        kafkaContainerHelper.sendOgVentTilKonsumert(
            sykefraværsstatistikk.toJsonKey(),
            sykefraværsstatistikk.toJsonValue(),
            KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        )

        val statistikkQ12023 = hentVirksomhetStatistikk(
            orgnr = "987654321",
            kvartal = KVARTAL_2024_3,
        )
        statistikkQ12023.orgnr shouldBe "987654321"
        statistikkQ12023.tapteDagsverk bigDecimalShouldBe 17.5
        statistikkQ12023.muligeDagsverk bigDecimalShouldBe 761.3
        statistikkQ12023.prosent bigDecimalShouldBe 2.3
        statistikkQ12023.antallPersoner shouldBe 4
        statistikkQ12023.tapteDagsverkGradertSykemelding bigDecimalShouldBe 33.2

        val statistikkMedVarighet = hentStatistikkMedVarighet(
            tabellnavn = "sykefravarsstatistikk_virksomhet_med_varighet",
            kolonnenavn = "orgnr",
            verdi = "987654321",
            årstallOgKvartal = KVARTAL_2024_3,
        )

        statistikkMedVarighet.tapteDagsverkMedVarighet shouldBe listOf(
            TapteDagsverkPerVarighet(
                varighet = "A",
                tapteDagsverk = 12.3,
            ),
            TapteDagsverkPerVarighet(
                varighet = "D",
                tapteDagsverk = 5.2,
            ),
        )
    }
}
