package no.nav.pia.sykefravarsstatistikk.domene

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.persistering.KvartalsvisSykefraværshistorikkService.Companion.ANTALL_ÅR_I_HISTORIKK
import kotlin.test.Test

class ÅrstallOgKvartalUnitTest {
    @Test
    fun `regner ut lista av ÅrstallOgKvartal`() {
        val liste = ÅrstallOgKvartal(2024, 4).inkludertTidligere(ANTALL_ÅR_I_HISTORIKK * 4)
        liste.size shouldBe 21
        liste.first() shouldBe ÅrstallOgKvartal(2024, 4)
        liste.last() shouldBe ÅrstallOgKvartal(2019, 4)
    }

    @Test
    fun `kalkulerer årstall og kvartal X år tilbake i tid`() {
        ÅrstallOgKvartal(2024, 4).førsteÅrstallOgKvartalSiden(ANTALL_ÅR_I_HISTORIKK) shouldBe ÅrstallOgKvartal(2020, 1)
        ÅrstallOgKvartal(2025, 1).førsteÅrstallOgKvartalSiden(ANTALL_ÅR_I_HISTORIKK) shouldBe ÅrstallOgKvartal(2020, 2)
        ÅrstallOgKvartal(2025, 2).førsteÅrstallOgKvartalSiden(ANTALL_ÅR_I_HISTORIKK) shouldBe ÅrstallOgKvartal(2020, 3)
        ÅrstallOgKvartal(2025, 3).førsteÅrstallOgKvartalSiden(ANTALL_ÅR_I_HISTORIKK) shouldBe ÅrstallOgKvartal(2020, 4)
        ÅrstallOgKvartal(2025, 4).førsteÅrstallOgKvartalSiden(ANTALL_ÅR_I_HISTORIKK) shouldBe ÅrstallOgKvartal(2021, 1)
    }
}
