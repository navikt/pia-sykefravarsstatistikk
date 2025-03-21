package no.nav.pia.sykefravarsstatistikk.persistering

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import no.nav.pia.sykefravarsstatistikk.api.dto.PubliseringskalenderDto
import kotlin.test.Test

class PubliseringsdatoDtoUnitTest {
    @Test
    fun `Sjekk uthenting av publiseringsdatoer til endepunkt`() {
        val publiseringsdatoer = listOf(
            PubliseringsdatoDto(
                rapportPeriode = "202503",
                offentligDato = LocalDateTime.parse("2025-11-27T08:00:00"),
                oppdatertIDvh = LocalDateTime.parse("2024-10-11T11:05:40"),
            ),
            PubliseringsdatoDto(
                rapportPeriode = "202502",
                offentligDato = LocalDateTime.parse("2025-09-01T08:00:00"),
                oppdatertIDvh = LocalDateTime.parse("2024-10-11T11:05:40"),
            ),
            PubliseringsdatoDto(
                rapportPeriode = "202501",
                offentligDato = LocalDateTime.parse("2025-06-01T08:00:00"),
                oppdatertIDvh = LocalDateTime.parse("2024-10-11T11:05:40"),
            ),
            PubliseringsdatoDto(
                rapportPeriode = "202404",
                offentligDato = LocalDateTime.parse("2025-02-27T08:00:00"),
                oppdatertIDvh = LocalDateTime.parse("2024-10-11T11:05:40"),
            ),
        )

        publiseringsdatoer.tilPubliseringskalender(dagensDato = LocalDateTime.parse("2025-03-18T08:15:00")) shouldBe
            PubliseringskalenderDto(
                sistePubliseringsdato = kotlinx.datetime.LocalDate.parse("2025-02-27"),
                nestePubliseringsdato = kotlinx.datetime.LocalDate.parse("2025-06-01"),
                gjeldendePeriode = no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal(2024, 4),
            )

        publiseringsdatoer.tilPubliseringskalender(dagensDato = LocalDateTime.parse("2025-06-01T08:15:00")) shouldBe
            PubliseringskalenderDto(
                sistePubliseringsdato = kotlinx.datetime.LocalDate.parse("2025-06-01"),
                nestePubliseringsdato = kotlinx.datetime.LocalDate.parse("2025-09-01"),
                gjeldendePeriode = no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal(2025, 1),
            )
    }
}
