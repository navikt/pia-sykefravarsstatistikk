package no.nav.pia.sykefravarsstatistikk.api.auth

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.AltinnTilgang
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.AltinnTilganger
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harEnkeltTilgang
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harTilgangTilOrgnr
import kotlin.test.Test

class AltinnTilgangerServiceUnitTest {
    companion object {
        val underenhet = "987654321"
        val overordnetEnhet = "3456789012"
    }

    @Test
    fun `utledder tilganger for en bruker som har ingen tilgang til underenheten`() {
        val altinnTilganger = AltinnTilganger(
            hierarki = listOf(
                AltinnTilgang(
                    overordnetEnhet,
                    altinn3Tilganger = emptySet(),
                    altinn2Tilganger = emptySet(),
                    navn = "Overordnet enhet",
                    organisasjonsform = "ORG",
                    underenheter = listOf(
                        AltinnTilgang(
                            "999888777",
                            altinn3Tilganger = emptySet(),
                            altinn2Tilganger = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                            navn = "Underenhet",
                            underenheter = emptyList(),
                            organisasjonsform = "BEDR",
                        ),
                    ),
                ),
            ),
            orgNrTilTilganger = mapOf(
                underenhet to emptySet(),
                overordnetEnhet to emptySet(),
            ),
            tilgangTilOrgNr = emptyMap(),
            isError = false,
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe false
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltTilgang(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
        altinnTilganger.harEnkeltTilgang(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
    }

    @Test
    fun `utledder riktige tilganger for en bruker som har ingen enkelrettighet til verken underenhet eller overordnet enhet`() {
        val altinnTilganger = AltinnTilganger(
            hierarki = listOf(
                AltinnTilgang(
                    overordnetEnhet,
                    altinn3Tilganger = emptySet(),
                    altinn2Tilganger = emptySet(),
                    navn = "Overordnet enhet",
                    organisasjonsform = "ORG",
                    underenheter = listOf(
                        AltinnTilgang(
                            underenhet,
                            altinn3Tilganger = emptySet(),
                            altinn2Tilganger = emptySet(),
                            navn = "Underenhet",
                            underenheter = emptyList(),
                            organisasjonsform = "BEDR",
                        ),
                    ),
                ),
            ),
            orgNrTilTilganger = mapOf(
                underenhet to emptySet(),
                overordnetEnhet to emptySet(),
            ),
            tilgangTilOrgNr = emptyMap(),
            isError = false,
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe true
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltTilgang(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
        altinnTilganger.harEnkeltTilgang(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
    }

    @Test
    fun `utledder riktige tilgangerfor en bruker som bare har enkelrettighet til underenhet`() {
        val altinnTilganger = AltinnTilganger(
            hierarki = listOf(
                AltinnTilgang(
                    overordnetEnhet,
                    altinn3Tilganger = emptySet(),
                    altinn2Tilganger = emptySet(),
                    navn = "Overordnet enhet",
                    organisasjonsform = "ORG",
                    underenheter = listOf(
                        AltinnTilgang(
                            underenhet,
                            altinn3Tilganger = emptySet(),
                            altinn2Tilganger = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                            navn = "Underenhet",
                            underenheter = emptyList(),
                            organisasjonsform = "BEDR",
                        ),
                    ),
                ),
            ),
            orgNrTilTilganger = mapOf(
                underenhet to setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
            ),
            tilgangTilOrgNr = mapOf(
                ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK to setOf(underenhet),
            ),
            isError = false,
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe true
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltTilgang(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe true
        altinnTilganger.harEnkeltTilgang(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
    }

    @Test
    fun `utledder riktige tilganger for en bruker som har enkelrettighet til både underenhet og overordnet enhet`() {
        val altinnTilganger = AltinnTilganger(
            hierarki = listOf(
                AltinnTilgang(
                    overordnetEnhet,
                    altinn3Tilganger = emptySet(),
                    altinn2Tilganger = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                    navn = "Overordnet enhet",
                    organisasjonsform = "ORG",
                    underenheter = listOf(
                        AltinnTilgang(
                            underenhet,
                            altinn3Tilganger = emptySet(),
                            altinn2Tilganger = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                            navn = "Underenhet",
                            underenheter = emptyList(),
                            organisasjonsform = "BEDR",
                        ),
                    ),
                ),
            ),
            orgNrTilTilganger = mapOf(
                underenhet to setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                overordnetEnhet to setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
            ),
            tilgangTilOrgNr = mapOf(
                ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK to setOf(underenhet, overordnetEnhet),
            ),
            isError = false,
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe true
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltTilgang(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe true
        altinnTilganger.harEnkeltTilgang(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe true
    }
}
