package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.AltinnOrganisasjon
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetINæringUtvinningAvRåoljeOgGass
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeSykehjem
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtvinningAvRåoljeOgGass
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.BeforeTest
import kotlin.test.Test

class OrganisasjonerEndepunktTest {
    @BeforeTest
    fun setup() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
        }
    }

    @Test
    fun `Innlogget bruker får hente organisasjoner hen har tilgang til`() {
        val underenhet: Underenhet = underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende()
        val overordnetEnhet: OverordnetEnhet = overordnetEnhetINæringUtvinningAvRåoljeOgGass.somOverordnetEnhet()
        altinnTilgangerContainerHelper.leggTilRettigheter(
            overordnetEnhet = overordnetEnhet,
            underenhet = underenhet,
            altinn2Rettighet = "En annen enkeltrettighet",
        )

        runBlocking {
            val response = TestContainerHelper.hentOrganisasjonerTilgangResponse(
                config = withToken(),
            )

            response.status.value shouldBe 200
            val altinnOrganisasjoner = Json.decodeFromString<List<AltinnOrganisasjon>>(response.bodyAsText())
            altinnOrganisasjoner shouldNotBe null
            altinnOrganisasjoner.size shouldBe 2
            val altinnOrganisasjonForOverordnetEnhet: AltinnOrganisasjon =
                altinnOrganisasjoner.find { it.organizationNumber == overordnetEnhet.orgnr }!!
            val altinnOrganisasjonForUnderenhet: AltinnOrganisasjon =
                altinnOrganisasjoner.find { it.organizationNumber == underenhet.orgnr }!!
            altinnOrganisasjonForOverordnetEnhet.name shouldBe overordnetEnhet.navn
            altinnOrganisasjonForOverordnetEnhet.organizationNumber shouldBe overordnetEnhet.orgnr
            altinnOrganisasjonForOverordnetEnhet.parentOrganizationNumber shouldBe ""
            altinnOrganisasjonForOverordnetEnhet.organizationForm shouldBe "ORGL"
            altinnOrganisasjonForUnderenhet.name shouldBe (underenhet as Underenhet.Næringsdrivende).navn
            altinnOrganisasjonForUnderenhet.organizationNumber shouldBe underenhet.orgnr
            altinnOrganisasjonForUnderenhet.parentOrganizationNumber shouldBe overordnetEnhet.orgnr
            altinnOrganisasjonForUnderenhet.organizationForm shouldBe "BEDR"
        }
    }

    @Test
    fun `Innlogget bruker får hente organisasjoner hen har enkeltrettigheter til`() {
        val underenhet: Underenhet = underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende()
        val overordnetEnhet: OverordnetEnhet = overordnetEnhetINæringUtvinningAvRåoljeOgGass.somOverordnetEnhet()
        altinnTilgangerContainerHelper.leggTilRettigheter(
            overordnetEnhet = overordnetEnhet,
            underenhet = underenhet,
            altinn2Rettighet = "En annen enkeltrettighet",
        )

        runBlocking {
            val response = TestContainerHelper.hentOrganisasjonerMedEnkeltrettighetResponse(
                config = withToken(),
            )

            response.status.value shouldBe 200
            val altinnOrganisasjoner = Json.decodeFromString<List<AltinnOrganisasjon>>(response.bodyAsText())
            altinnOrganisasjoner shouldNotBe null
            altinnOrganisasjoner.size shouldBe 0
        }
    }

    @Test
    fun `Innlogget bruker får hente organisasjoner hen har enkeltrettigheter til (Altinn2)`() {
        val underenhet: Underenhet = underenhetIBransjeSykehjem.somNæringsdrivende()
        val overordnetEnhet: OverordnetEnhet = overordnetEnhetINæringUtvinningAvRåoljeOgGass.somOverordnetEnhet()
        altinnTilgangerContainerHelper.leggTilRettigheter(
            overordnetEnhet = overordnetEnhet,
            underenhet = underenhet,
            altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2,
        )

        runBlocking {
            val response = TestContainerHelper.hentOrganisasjonerMedEnkeltrettighetResponse(
                config = withToken(),
            )

            response.status.value shouldBe 200
            val altinnOrganisasjoner = Json.decodeFromString<List<AltinnOrganisasjon>>(response.bodyAsText())
            altinnOrganisasjoner shouldNotBe null
            altinnOrganisasjoner.size shouldBe 1
            altinnOrganisasjoner.find { it.organizationNumber == overordnetEnhet.orgnr } shouldBe null
            val altinnOrganisasjonForUnderenhet: AltinnOrganisasjon =
                altinnOrganisasjoner.find { it.organizationNumber == underenhet.orgnr }!!
            altinnOrganisasjonForUnderenhet.name shouldBe (underenhet as Underenhet.Næringsdrivende).navn
            altinnOrganisasjonForUnderenhet.organizationNumber shouldBe underenhet.orgnr
            altinnOrganisasjonForUnderenhet.parentOrganizationNumber shouldBe overordnetEnhet.orgnr
            altinnOrganisasjonForUnderenhet.organizationForm shouldBe "BEDR"
        }
    }

    @Test
    fun `Innlogget bruker får hente organisasjoner hen har enkeltrettigheter til (Altinn3)`() {
        val underenhet: Underenhet = underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende()
        val overordnetEnhet: OverordnetEnhet = overordnetEnhetINæringUtvinningAvRåoljeOgGass.somOverordnetEnhet()
        altinnTilgangerContainerHelper.leggTilRettigheter(
            overordnetEnhet = overordnetEnhet,
            underenhet = underenhet,
            altinn2Rettighet = "En annen enkeltrettighet",
            altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
        )

        runBlocking {
            val response = TestContainerHelper.hentOrganisasjonerMedEnkeltrettighetResponse(
                config = withToken(),
            )

            response.status.value shouldBe 200
            val altinnOrganisasjoner = Json.decodeFromString<List<AltinnOrganisasjon>>(response.bodyAsText())
            altinnOrganisasjoner shouldNotBe null
            altinnOrganisasjoner.size shouldBe 1
            altinnOrganisasjoner.find { it.organizationNumber == overordnetEnhet.orgnr } shouldBe null
            val altinnOrganisasjonForUnderenhet: AltinnOrganisasjon =
                altinnOrganisasjoner.find { it.organizationNumber == underenhet.orgnr }!!
            altinnOrganisasjonForUnderenhet.name shouldBe (underenhet as Underenhet.Næringsdrivende).navn
            altinnOrganisasjonForUnderenhet.organizationNumber shouldBe underenhet.orgnr
            altinnOrganisasjonForUnderenhet.parentOrganizationNumber shouldBe overordnetEnhet.orgnr
            altinnOrganisasjonForUnderenhet.organizationForm shouldBe "BEDR"
        }
    }
}
