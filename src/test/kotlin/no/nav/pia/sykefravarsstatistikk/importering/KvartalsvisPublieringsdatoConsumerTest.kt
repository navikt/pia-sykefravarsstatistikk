package no.nav.pia.sykefravarsstatistikk.importering

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import no.nav.pia.sykefravarsstatistikk.helper.PubliseringsdatoImportTestUtils.Companion.hentPubliseringsdato
import no.nav.pia.sykefravarsstatistikk.helper.PubliseringsdatoImportTestUtils.PubliseringsdatoJsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
import kotlin.test.Test

class KvartalsvisPublieringsdatoConsumerTest {
    @Test
    fun `Kafkamelding om metadata for PUBLISERINGSDATO blir lagret i DB`() {
        val publiseringsdato = PubliseringsdatoJsonMelding(
            rapportPeriode = "202404",
            offentligDato = LocalDateTime.parse("2025-02-27T08:00:00"),
            oppdatertIDvh = LocalDateTime.parse("2024-11-19T08:00:00"),
        )

        kafkaContainerHelper.sendOgVentTilKonsumert(
            publiseringsdato.toJsonKey(),
            publiseringsdato.toJsonValue(),
            Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_PUBLISERINGSDATO,
        )

        val publiseringsdatoQ42024 = hentPubliseringsdato(
            rapportPeriode = "202404",
        )

        publiseringsdatoQ42024.rapportPeriode shouldBe "202404"
        publiseringsdatoQ42024.offentligDato shouldBe LocalDateTime.parse("2025-02-27T08:00:00")
        publiseringsdatoQ42024.oppdatertIDvh shouldBe LocalDateTime.parse("2024-11-19T08:00:00")
    }
}
