package no.nav.pia.sykefravarsstatistikk.api

import ia.felles.definisjoner.bransjer.Bransje
import io.kotest.inspectors.shouldForNone
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enOverordnetEnhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetUtenStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enVirksomhetUtenTilgangIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

class SykefraværsstatistikkApiEndepunkterTest {
    @BeforeTest
    fun cleanUp() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
        }
    }

    @Test
    fun `Bruker som når et ukjent endepunkt får '404 - Not found' i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetIAltinn.orgnr}/sykefravarshistorikk/alt",
            )
            resultat?.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
            )
            resultat?.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )
            resultat?.status shouldBe HttpStatusCode.Forbidden
        }
    }

    @Test
    fun `Innlogget bruker uten enkelrettighet til virksomhet får '200 - OK', men ingen statistikk for virksomhet`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = enUnderenhetUtenStatistikk.orgnr,
                altinn2Rettighet = "ingen_tilgang_til_statistikk",
            )
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendBransjestatistikk(bransje = Bransje.BARNEHAGER)

            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetUtenStatistikk.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )
            resultat!!.status shouldBe HttpStatusCode.OK
            val aggregertStatistikk: List<KvartalsvisSykefraværshistorikkDto> = resultat.body()

            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.LAND.name }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.SEKTOR.name }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.BRANSJE.name }

            aggregertStatistikk.shouldForNone { statistikk -> statistikk.type shouldBe Statistikkategori.VIRKSOMHET.name }
            aggregertStatistikk.shouldForNone { statistikk -> statistikk.type shouldBe Statistikkategori.OVERORDNET_ENHET.name }
        }
    }

    @Test
    fun `Innlogget bruker får en 200`() {
        runBlocking {
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendBransjestatistikk(bransje = Bransje.SYKEHJEM)
            kafkaContainerHelper.sendVirksomhetsstatistikk(orgnr = enUnderenhetIAltinn.orgnr)
            // TODO: Returner en tom liste for virksomhet i stedet for feil

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = enUnderenhetIAltinn.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `Får feil ved manglende statistikk`() {
        // TODO: mulig det ikke skal bli feil, men heller tom liste om statistikk mangler
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = enUnderenhetUtenStatistikk.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            val responseManglerAltAvStatistikk = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetUtenStatistikk.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            responseManglerAltAvStatistikk.shouldNotBeNull()
            responseManglerAltAvStatistikk.status shouldNotBe HttpStatusCode.OK
            kafkaContainerHelper.sendLandsstatistikk()

            val responseManglerAltUtenomLand = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetUtenStatistikk.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            responseManglerAltUtenomLand.shouldNotBeNull()
            responseManglerAltUtenomLand.status shouldNotBe HttpStatusCode.OK

            kafkaContainerHelper.sendSektorstatistikk()

            val responseManglerAltUtenomLandOgSektor = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetUtenStatistikk.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            responseManglerAltUtenomLandOgSektor.shouldNotBeNull()
            responseManglerAltUtenomLandOgSektor.status shouldNotBe HttpStatusCode.OK

            kafkaContainerHelper.sendBransjestatistikk(bransje = Bransje.BARNEHAGER)

            val responseManglerVirksomhet = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetUtenStatistikk.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            responseManglerVirksomhet.shouldNotBeNull()
            responseManglerVirksomhet.status shouldNotBe HttpStatusCode.OK

            kafkaContainerHelper.sendVirksomhetsstatistikk(orgnr = enUnderenhetUtenStatistikk.orgnr)

            val harAltAvStatistikk = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetUtenStatistikk.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )
            harAltAvStatistikk.shouldNotBeNull()
            harAltAvStatistikk.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    @Ignore
    fun `Bruker med tilhørighet til virksomhet får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendVirksomhetsstatistikk(orgnr = enVirksomhetUtenTilgangIAltinn.orgnr)

            val resultat: HttpResponse? = TestContainerHelper.applikasjon.performGet(
                url = "/${enVirksomhetUtenTilgangIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.OK

            TestContainerHelper.applikasjon.shouldContainLog(
                "${enUnderenhetIAltinn.orgnr} er ikke en del av en bransje".toRegex(),
            )

            val aggregertStatistikk: List<KvartalsvisSykefraværshistorikkDto> = resultat.body()

            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "OVERORDNET_ENHET" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "VIRKSOMHET" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "LAND" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "SEKTOR" }

            val virksomhet = aggregertStatistikk.first { it.type == "VIRKSOMHET" }
            virksomhet.label shouldBe enUnderenhetIAltinn.navn
            virksomhet.kvartalsvisSykefraværsprosent.size shouldBe 0

            val landStatistikk = aggregertStatistikk.first { it.type == "LAND" }
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år

            val sektorStatistikk = aggregertStatistikk.first { it.type == "SEKTOR" }
            sektorStatistikk.label shouldBe "1"

            // Tapte dagsverk
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000

            // Mulige dagsverk
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            // Prosent
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
        }
    }

    @Test
    @Ignore
    fun `Bruker med tilhørighet til virksomhet (i bransje) får kvartalsvis statistikk`() {
        TODO("Skal kunne se bransje, land og sektor?")
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet og enkelttilgang får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendVirksomhetsstatistikk(orgnr = enUnderenhetIAltinn.orgnr)

            val resultat: HttpResponse? = TestContainerHelper.applikasjon.performGet(
                url = "/${enUnderenhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            TestContainerHelper.applikasjon.shouldContainLog(
                "${enUnderenhetIAltinn.orgnr} er ikke en del av en bransje".toRegex(),
            )

            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.OK

            val aggregertStatistikk: List<KvartalsvisSykefraværshistorikkDto> = resultat.body()

            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "OVERORDNET_ENHET" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "VIRKSOMHET" }
//            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "BRANSJE" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "LAND" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "SEKTOR" }

            val virksomhet = aggregertStatistikk.first { it.type == "VIRKSOMHET" }
            virksomhet.label shouldBe enUnderenhetIAltinn.navn

            val overordnetEnhet = aggregertStatistikk.first { it.type == "OVERORDNET_ENHET" }
            overordnetEnhet.label shouldBe enOverordnetEnhetIAltinn.navn
            overordnetEnhet.kvartalsvisSykefraværsprosent.size shouldBe 0

//            val bransjeStatistikk = aggregertStatistikk.first { it.type == "BRANSJE" }
//            bransjeStatistikk.label shouldBe bransje

            val landStatistikk = aggregertStatistikk.first { it.type == "LAND" }
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år

            val sektorStatistikk = aggregertStatistikk.first { it.type == "SEKTOR" }
            sektorStatistikk.label shouldBe "1" // TODO: Burde ikke være hardkodet i route

            // Tapte dagsverk
            virksomhet.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 154.5439
//            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 270744.659570
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000

            // Mulige dagsverk
            virksomhet.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 761.3
//            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 4668011.371895
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            // Prosent
            virksomhet.kvartalsvisSykefraværsprosent.first().prosent shouldBe 28.3
//            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.8
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
        }
    }

    @Test
    @Ignore
    fun `Bruker med tilhørighet til virksomhet (i bransje) og enkelttilgang får kvartalsvis statistikk`() {
        TODO("Skal kunne se virksomhet,bransje, land og sektor?")
    }

    @Test
    @Ignore
    fun `Bruker med tilhørighet til virksomhet, enkelttilgang og overordnet enhet får kvartalsvis statistikk`() {
        TODO("Skal kunne se overordnetEnhet, virksomhet, land og sektor?")
    }

    @Test
    @Ignore
    fun `Bruker med tilhørighet til virksomhet (i bransje), enkelttilgang og overordnet enhet får kvartalsvis statistikk`() {
        TODO("Skal kunne se overordnetEnhet, virksomhet, bransje, land og sektor?")
    }
}
