package no.nav.pia.sykefravarsstatistikk.api.auth

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.AltinnTilgang
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.AltinnTilganger
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.altinnOrganisasjonerVedkommendeHarTilgangTil
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.finnOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harEnkeltrettighet
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harTilgangTilOrgnr
import no.nav.pia.sykefravarsstatistikk.domene.AltinnOrganisasjon
import kotlin.test.Test

class AltinnTilgangerServiceUnitTest {
    companion object {
        val overordnetEnhet = "3456789012"
        val underenhet = "987654321"
        val underenhet2 = "789789789"
        val underenhetNivå2 = "222222222"
        val underenhetNivå3 = "333333333"
    }

    @Test
    fun `utledder tilganger for en bruker som har ingen tilgang til underenheten (men har tilgang til en annen virksomhet)`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = emptySet(),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = "989898989",
                    navn = "Ikke den underenheten vi leter etter",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe false
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltrettighet(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
        altinnTilganger.harEnkeltrettighet(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
    }

    @Test
    fun `utledder riktige tilganger for en bruker som har ingen enkelrettighet til verken underenhet eller overordnet enhet`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = emptySet(),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = emptySet(),
                ),
            ),
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe true
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltrettighet(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
        altinnTilganger.harEnkeltrettighet(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
    }

    @Test
    fun `utledder riktige tilganger for en bruker som bare har enkelrettighet til underenhet`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = emptySet(),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe true
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltrettighet(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe true
        altinnTilganger.harEnkeltrettighet(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe false
    }

    @Test
    fun `utledder riktige tilganger for en bruker som har enkelrettighet til både underenhet og overordnet enhet`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
        )

        altinnTilganger.harTilgangTilOrgnr(underenhet) shouldBe true
        altinnTilganger.harTilgangTilOrgnr(overordnetEnhet) shouldBe true
        altinnTilganger.harEnkeltrettighet(orgnr = underenhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe true
        altinnTilganger.harEnkeltrettighet(orgnr = overordnetEnhet, ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe true
    }

    @Test
    fun `listen av virksomheter en bruker har tilgang til er tom`() {
        val altinnTilganger = AltinnTilganger(
            hierarki = emptyList(),
            orgNrTilTilganger = emptyMap(),
            tilgangTilOrgNr = emptyMap(),
            isError = false,
        )

        altinnTilganger.altinnOrganisasjonerVedkommendeHarTilgangTil() shouldBe emptyList()
        altinnTilganger.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe emptyList()
    }

    @Test
    fun `listen av AltinnVirksomheter en bruker har tilgang til inneholder riktige virksomheter`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = emptySet(),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
        )

        altinnTilganger.altinnOrganisasjonerVedkommendeHarTilgangTil() shouldContainExactlyInAnyOrder listOf(
            AltinnOrganisasjon(
                name = "Underenhet",
                organizationNumber = underenhet,
                organizationForm = "BEDR",
                parentOrganizationNumber = overordnetEnhet,
            ),
            AltinnOrganisasjon(
                name = "Overordnet enhet",
                organizationNumber = overordnetEnhet,
                organizationForm = "ORG",
                parentOrganizationNumber = "",
            ),
        )
        altinnTilganger.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK) shouldBe listOf(
            AltinnOrganisasjon(
                name = "Underenhet",
                organizationNumber = underenhet,
                organizationForm = "BEDR",
                parentOrganizationNumber = overordnetEnhet,
            ),
        )
    }

    @Test
    fun `listen av virksomheter en bruker har tilgang til hentes rekursivt`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet2,
                    navn = "Underenhet 2",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
            underenheterNivå2 = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhetNivå2,
                    navn = "Underenhet nivå 2",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
        )

        altinnTilganger.altinnOrganisasjonerVedkommendeHarTilgangTil() shouldContainExactlyInAnyOrder listOf(
            AltinnOrganisasjon(
                name = "Underenhet",
                organizationNumber = underenhet,
                organizationForm = "BEDR",
                parentOrganizationNumber = overordnetEnhet,
            ),
            AltinnOrganisasjon(
                name = "Underenhet 2",
                organizationNumber = underenhet2,
                organizationForm = "BEDR",
                parentOrganizationNumber = overordnetEnhet,
            ),
            AltinnOrganisasjon(
                name = "Underenhet nivå 2",
                organizationNumber = underenhetNivå2,
                organizationForm = "BEDR",
                parentOrganizationNumber = underenhet,
            ),
            AltinnOrganisasjon(
                name = "Overordnet enhet",
                organizationNumber = overordnetEnhet,
                organizationForm = "ORG",
                parentOrganizationNumber = "",
            ),
        )
        altinnTilganger.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil(
            ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
        ) shouldContainExactlyInAnyOrder
            listOf(
                AltinnOrganisasjon(
                    name = "Underenhet",
                    organizationNumber = underenhet,
                    organizationForm = "BEDR",
                    parentOrganizationNumber = overordnetEnhet,
                ),
                AltinnOrganisasjon(
                    name = "Underenhet 2",
                    organizationNumber = underenhet2,
                    organizationForm = "BEDR",
                    parentOrganizationNumber = overordnetEnhet,
                ),
                AltinnOrganisasjon(
                    name = "Underenhet nivå 2",
                    organizationNumber = underenhetNivå2,
                    organizationForm = "BEDR",
                    parentOrganizationNumber = underenhet,
                ),
                AltinnOrganisasjon(
                    name = "Overordnet enhet",
                    organizationNumber = overordnetEnhet,
                    organizationForm = "ORG",
                    parentOrganizationNumber = "",
                ),
            )
    }

    @Test
    fun `listen av virksomheter en bruker har tilgang til hentes rekursivt -- med og uten tilgang`() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = setOf("En annen enkelrettighet"),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf("En annen enkelrettighet"),
                ),
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet2,
                    navn = "Underenhet 2",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
            underenheterNivå2 = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhetNivå2,
                    navn = "Underenhet nivå 2",
                    enkeltrettigheter = setOf("En annen enkelrettighet"),
                ),
            ),
            underenheterNivå3 = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhetNivå3,
                    navn = "Underenhet nivå 3",
                    enkeltrettigheter = setOf(ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK),
                ),
            ),
        )

        altinnTilganger.altinnOrganisasjonerVedkommendeHarTilgangTil() shouldContainExactlyInAnyOrder listOf(
            AltinnOrganisasjon(
                name = "Underenhet",
                organizationNumber = underenhet,
                organizationForm = "BEDR",
                parentOrganizationNumber = overordnetEnhet,
            ),
            AltinnOrganisasjon(
                name = "Underenhet 2",
                organizationNumber = underenhet2,
                organizationForm = "BEDR",
                parentOrganizationNumber = overordnetEnhet,
            ),
            AltinnOrganisasjon(
                name = "Underenhet nivå 2",
                organizationNumber = underenhetNivå2,
                organizationForm = "BEDR",
                parentOrganizationNumber = underenhet,
            ),
            AltinnOrganisasjon(
                name = "Underenhet nivå 3",
                organizationNumber = underenhetNivå3,
                organizationForm = "BEDR",
                parentOrganizationNumber = underenhetNivå2,
            ),
            AltinnOrganisasjon(
                name = "Overordnet enhet",
                organizationNumber = overordnetEnhet,
                organizationForm = "ORG",
                parentOrganizationNumber = "",
            ),
        )
        altinnTilganger.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil(
            ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
        ) shouldContainExactlyInAnyOrder
            listOf(
                AltinnOrganisasjon(
                    name = "Underenhet 2",
                    organizationNumber = underenhet2,
                    organizationForm = "BEDR",
                    parentOrganizationNumber = overordnetEnhet,
                ),
                AltinnOrganisasjon(
                    name = "Underenhet nivå 3",
                    organizationNumber = underenhetNivå3,
                    organizationForm = "BEDR",
                    parentOrganizationNumber = underenhetNivå2,
                ),
            )
    }

    @Test
    fun `finn overordnet enhet (parent organization) av en enhet -- på første nivå `() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = setOf("En annen enkelrettighet"),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf("En annen enkelrettighet"),
                ),
            ),
        )

        altinnTilganger.finnOverordnetEnhet(underenhet) shouldBe overordnetEnhet
    }

    @Test
    fun `finn overordnet enhet (parent organization) av en enhet `() {
        val altinnTilganger = lagAltinnTilganger(
            overordnetEnhet = EnkeltrettigheterTilEnVirksomhet(
                orgnr = overordnetEnhet,
                navn = "Overordnet enhet",
                enkeltrettigheter = setOf("En annen enkelrettighet"),
            ),
            underenheter = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhet,
                    navn = "Underenhet",
                    enkeltrettigheter = setOf("En annen enkelrettighet"),
                ),
            ),
            underenheterNivå2 = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhetNivå2,
                    navn = "Underenhet nivå 2",
                    enkeltrettigheter = setOf("En annen enkelrettighet"),
                ),
            ),
            underenheterNivå3 = listOf(
                EnkeltrettigheterTilEnVirksomhet(
                    orgnr = underenhetNivå3,
                    navn = "Underenhet nivå 3",
                    enkeltrettigheter = setOf("En annen enkelrettighet"),
                ),
            ),
        )

        altinnTilganger.finnOverordnetEnhet(underenhet) shouldBe overordnetEnhet
        altinnTilganger.finnOverordnetEnhet(underenhetNivå2) shouldBe underenhet
        altinnTilganger.finnOverordnetEnhet(underenhetNivå3) shouldBe underenhetNivå2
    }

    private fun lagAltinnTilganger(
        overordnetEnhet: EnkeltrettigheterTilEnVirksomhet,
        underenheter: List<EnkeltrettigheterTilEnVirksomhet>,
        underenheterNivå2: List<EnkeltrettigheterTilEnVirksomhet> = emptyList(),
        underenheterNivå3: List<EnkeltrettigheterTilEnVirksomhet> = emptyList(),
    ): AltinnTilganger =
        AltinnTilganger(
            hierarki = listOf(
                AltinnTilgang(
                    overordnetEnhet.orgnr,
                    altinn2Tilganger = overordnetEnhet.enkeltrettigheter,
                    altinn3Tilganger = overordnetEnhet.enkeltrettigheterAltinn3,
                    navn = overordnetEnhet.navn,
                    organisasjonsform = "ORG",
                    underenheter = listOf(
                        AltinnTilgang(
                            orgnr = underenheter.first().orgnr,
                            altinn2Tilganger = underenheter.first().enkeltrettigheter,
                            altinn3Tilganger = underenheter.first().enkeltrettigheterAltinn3,
                            navn = underenheter.first().navn,
                            underenheter = if (underenheterNivå2.isNotEmpty()) {
                                underenheterNivå2.map { underenhetNivå2 ->
                                    AltinnTilgang(
                                        orgnr = underenhetNivå2.orgnr,
                                        altinn2Tilganger = underenhetNivå2.enkeltrettigheter,
                                        altinn3Tilganger = underenhetNivå2.enkeltrettigheterAltinn3,
                                        navn = underenhetNivå2.navn,
                                        underenheter = if (underenheterNivå3.isNotEmpty()) {
                                            underenheterNivå3.map { underenhetNivå3 ->
                                                AltinnTilgang(
                                                    orgnr = underenhetNivå3.orgnr,
                                                    altinn2Tilganger = underenhetNivå3.enkeltrettigheter,
                                                    altinn3Tilganger = underenhetNivå3.enkeltrettigheterAltinn3,
                                                    navn = underenhetNivå3.navn,
                                                    underenheter = emptyList(),
                                                    organisasjonsform = "BEDR",
                                                )
                                            }
                                        } else {
                                            emptyList()
                                        },
                                        organisasjonsform = "BEDR",
                                    )
                                }
                            } else {
                                emptyList()
                            },
                            organisasjonsform = "BEDR",
                        ),
                    ).plus(
                        underenheter.drop(1).map { underenhet ->
                            AltinnTilgang(
                                orgnr = underenhet.orgnr,
                                altinn2Tilganger = underenhet.enkeltrettigheter,
                                altinn3Tilganger = underenhet.enkeltrettigheterAltinn3,
                                navn = underenhet.navn,
                                underenheter = emptyList(),
                                organisasjonsform = "BEDR",
                            )
                        },
                    ),
                ),
            ),
            orgNrTilTilganger = mapOf(
                overordnetEnhet.orgnr to overordnetEnhet.enkeltrettigheter,
            ).plus(underenheter.toPair()).plus(underenheterNivå2.toPair()),
            tilgangTilOrgNr = listOf(overordnetEnhet).plus(underenheter).plus(underenheterNivå2)
                .splitByEnkeltrettighet(),
            isError = false,
        )

    @Test
    fun `Selftest -- splitByEnkeltrettighet`() {
        val liste = listOf(
            EnkeltrettigheterTilEnVirksomhet(
                orgnr = "111111111",
                navn = "Test 1",
                enkeltrettigheter = setOf("ENKELRETTIGHET_1"),
            ),
            EnkeltrettigheterTilEnVirksomhet(
                orgnr = "222222222",
                navn = "Test 2",
                enkeltrettigheter = setOf("ENKELRETTIGHET_1", "ENKELRETTIGHET_2"),
            ),
            EnkeltrettigheterTilEnVirksomhet(
                orgnr = "333333333",
                navn = "Test 3",
                enkeltrettigheter = setOf("ENKELRETTIGHET_3", "ENKELRETTIGHET_2"),
            ),
            EnkeltrettigheterTilEnVirksomhet(
                orgnr = "444444444",
                navn = "Test 4",
                enkeltrettigheter = setOf("ENKELRETTIGHET_4"),
            ),
        )
        liste.splitByEnkeltrettighet() shouldBe mapOf(
            "ENKELRETTIGHET_1" to setOf("111111111", "222222222"),
            "ENKELRETTIGHET_2" to setOf("222222222", "333333333"),
            "ENKELRETTIGHET_3" to setOf("333333333"),
            "ENKELRETTIGHET_4" to setOf("444444444"),
        )
    }

    private data class EnkeltrettigheterTilEnVirksomhet(
        val orgnr: String,
        val navn: String,
        val enkeltrettigheter: Set<String>,
        val enkeltrettigheterAltinn3: Set<String> = emptySet(),
    )

    private fun List<EnkeltrettigheterTilEnVirksomhet>.toPair(): Map<String, Set<String>> = this.associate { it.toPair() }

    private fun EnkeltrettigheterTilEnVirksomhet.toPair(): Pair<String, Set<String>> = orgnr to enkeltrettigheter

    private fun List<EnkeltrettigheterTilEnVirksomhet>.splitByEnkeltrettighet(): Map<String, Set<String>> =
        this.associate { it.toPair() }.flatMap { (orgnr, enkeltrettigheter) ->
            enkeltrettigheter.map { enkeltrettighet -> enkeltrettighet to orgnr }
        }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }.toMap()
}
