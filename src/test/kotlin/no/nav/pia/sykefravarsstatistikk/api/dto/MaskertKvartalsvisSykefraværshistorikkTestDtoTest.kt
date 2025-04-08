package no.nav.pia.sykefravarsstatistikk.api.dto

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.maskering.MaskertKvartalsvisSykefraværshistorikkDto.Companion.tilMaskertBigDecimal
import no.nav.pia.sykefravarsstatistikk.api.maskering.MaskertKvartalsvisSykefraværsprosentDto
import kotlin.test.Test

class MaskertKvartalsvisSykefraværshistorikkTestDtoTest {
    @Test
    fun `Kan serialize en MaskertBigDecimal på den samme måten som en BigDecimal`() {
        val dto = MaskertKvartalsvisSykefraværsprosentDto(
            årstall = 2024,
            kvartal = 4,
            tapteDagsverk = 100.05.toBigDecimal().tilMaskertBigDecimal(),
            muligeDagsverk = 1000.0.toBigDecimal().tilMaskertBigDecimal(),
            prosent = 10.toBigDecimal().tilMaskertBigDecimal(),
            erMaskert = false,
        )
        val expectedJsonValue =
            """
            {
              "årstall": 2024,
              "kvartal": 4,
              "tapteDagsverk": 100.05,
              "muligeDagsverk": 1000.0,
              "prosent": 10,
              "erMaskert": false
            }
            """.replace(Regex("[\n\r]"), "").replace(Regex("\\p{Zs}+"), "")

        Json.encodeToString(dto) shouldBe expectedJsonValue
    }
}
