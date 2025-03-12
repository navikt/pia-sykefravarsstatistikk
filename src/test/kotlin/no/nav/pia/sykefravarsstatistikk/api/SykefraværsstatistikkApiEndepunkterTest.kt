package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.assertions.shouldFailWithMessage
import io.kotest.inspectors.forAll
import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetUtenStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedEnkelrettighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetBransjeBygg
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetBransjeSykehus
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetUtenBransje2
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetBransjeSykehus
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetUtenBransje2
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedTilhørighetBransjeBygg
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.BeforeTest
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
                url = "/${underenhetMedTilhørighetUtenBransje.orgnr}/sykefravarshistorikk/alt",
                config = withToken(),
            )
            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response`() {
        runBlocking {
            shouldFailWithMessage("Feil ved henting av kvartalsvis statistikk, status: 401 Unauthorized, message: ") {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                )
            }
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response`() {
        runBlocking {
            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 403 Forbidden, message: Bruker har ikke tilgang til virksomheten",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                    config = withToken(),
                )
            }
        }
    }

    @Test
    fun `Innlogget bruker uten enkelrettighet til virksomhet får '200 - OK', men ingen statistikk for virksomhet`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                altinn2Rettighet = "ingen_tilgang_til_statistikk",
            )
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage,
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                config = withToken(),
            )

            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.LAND.name }
            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.SEKTOR.name }
            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.BRANSJE.name }

            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.VIRKSOMHET.name }

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == Statistikkategori.VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeBarnehage.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0

            kvartalsvisStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe Statistikkategori.OVERORDNET_ENHET.name }

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == Statistikkategori.OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetBransjeBarnehage.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0
        }
    }

    @Test
    fun `Innlogget bruker får en 200`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetMedTilhørighetUtenBransje,
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje,
            )

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedTilhørighetUtenBransje.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                config = withToken(),
            ).shouldNotBeNull()
        }
    }

    @Test
    fun `Får feil ved manglende statistikk`() {
        // TODO Det er usannsynlig at statistikk mangler for land, sektor og bransje, best at de for tom liste i stedet for feil
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = enUnderenhetUtenStatistikk.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            // TODO: burde ikke feile, men heller sende en tom liste?
            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 400 Bad Request, message: Ingen bransjestatistikk funnet for bransje '${enUnderenhetUtenStatistikk.bransje()!!.navn}'",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = enUnderenhetUtenStatistikk.orgnr,
                    config = withToken(),
                )
            }

            val bransje = enUnderenhetUtenStatistikk.bransje()
            // Får ikke testet manglende statistikk om denne bransjen allerede har blitt sendt på kafka, som også er sannsynlig

            if (bransje != null) {
                kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
            } else {
                kafkaContainerHelper.sendNæringsstatistikk(næring = enUnderenhetUtenStatistikk.næringskode.næring)
            }

            // TODO: burde ikke feile, men heller sende en tom liste?
            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 400 Bad Request, message: Ingen virksomhetsstatistikk funnet for underenhet '${enUnderenhetUtenStatistikk.orgnr}'",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = enUnderenhetUtenStatistikk.orgnr,
                    config = withToken(),
                )
            }

            kafkaContainerHelper.sendVirksomhetsstatistikk(virksomhet = enUnderenhetUtenStatistikk)

            // TODO: Får ikke testet siden landsstatistikk og sektorstatistikk sannsynligvis allerede har blitt sendt på kafka
//            kafkaContainerHelper.sendSektorstatistikk(sektor = overordnetEnhetUtenStatistikk.sektor)
//            kafkaContainerHelper.sendLandsstatistikk()

            TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = enUnderenhetUtenStatistikk.orgnr,
                config = withToken(),
            ).shouldNotBeNull()
        }
    }

    @Test
    fun `Bruker med tilhørighet (ikke enkeltrettighet) til virksomhet får kvartalsvis statistikk`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedTilhørighetUtenBransje.orgnr,
            )
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetMedTilhørighetUtenBransje,
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                config = withToken(),
            )

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "SEKTOR" }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedTilhørighetUtenBransje.sektor.kode
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "NÆRING" }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe underenhetMedTilhørighetUtenBransje.næringskode.næring.tosifferIdentifikator
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "VIRKSOMHET" }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedTilhørighetUtenBransje.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "OVERORDNET_ENHET" }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedTilhørighetUtenBransje.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0
        }
    }

    @Test
    fun `Bruker med tilhørighet (ikke enkeltrettighet) til virksomhet (i bransje) får kvartalsvis statistikk`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedTilhørighetBransjeBygg.orgnr,
            )

            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedTilhørighetBransjeBygg,
                underenhet = underenhetMedTilhørighetBransjeBygg,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedTilhørighetBransjeBygg.orgnr,
                config = withToken(),
            )

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "SEKTOR" }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedTilhørighetBransjeBygg.sektor.kode
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            val bransje = underenhetMedTilhørighetBransjeBygg.bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "BRANSJE" }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "VIRKSOMHET" }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedTilhørighetBransjeBygg.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "OVERORDNET_ENHET" }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedTilhørighetBransjeBygg.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet og enkelttilgang får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje2,
                underenhet = underenhetMedEnkelrettighetUtenBransje,
            )

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetUtenBransje.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedEnkelrettighetUtenBransje.orgnr,
                config = withToken(),
            )

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "SEKTOR" }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedTilhørighetUtenBransje2.sektor.kode
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "NÆRING" }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje.næringskode.næring.tosifferIdentifikator
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "VIRKSOMHET" }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "OVERORDNET_ENHET" }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedTilhørighetUtenBransje2.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet (i bransje) og enkelttilgang får kvartalsvis statistikk`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeSykehus.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedTilhørighetBransjeSykehus,
                underenhet = underenhetMedEnkelrettighetBransjeSykehus,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedEnkelrettighetBransjeSykehus.orgnr,
                config = withToken(),
            )

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "SEKTOR" }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe "1"
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            val bransje = underenhetMedEnkelrettighetBransjeSykehus.bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "BRANSJE" }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "VIRKSOMHET" }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeSykehus.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "OVERORDNET_ENHET" }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedTilhørighetBransjeSykehus.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 0
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet, enkelttilgang og overordnet enhet får kvartalsvis statistikk`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetUtenBransje2.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedEnkelrettighetUtenBransje,
                underenhet = underenhetMedEnkelrettighetUtenBransje2,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedEnkelrettighetUtenBransje2.orgnr,
                config = withToken(),
            )

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "SEKTOR" }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetUtenBransje.sektor.kode
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "NÆRING" }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje2.næringskode.næring.tosifferIdentifikator
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "VIRKSOMHET" }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje2.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "OVERORDNET_ENHET" }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetUtenBransje.navn
//            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldNotBe 0
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet (i bransje), enkelttilgang og overordnet enhet får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage,
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage,
            )

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            // TODO: gi enkelrettighet på overordnet enhet
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                config = withToken(),
            )

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "SEKTOR" }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetBransjeBarnehage.sektor.kode
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            val bransje = underenhetMedEnkelrettighetBransjeBarnehage.bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "BRANSJE" }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "VIRKSOMHET" }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeBarnehage.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == "OVERORDNET_ENHET" }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetBransjeBarnehage.navn
//            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent.size shouldNotBe 0
        }
    }

    @Test
    fun `Bruker får aggregert statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage,
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage,
            )

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            val aggregertStatistikkDto = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                config = withToken(),
            )

            val bransje = underenhetMedEnkelrettighetBransjeBarnehage.bransje()!!

            val landStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == "LAND" }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalerIBeregningen.size shouldBe 4 // skal være ett år
            landStatistikk.kvartalerIBeregningen.forAll { it.årstall shouldBe 2024 }
            // TODO: finn en mer robust måte å teste hvilke kvartaler som er med i beregning, sjekk over flere år?
            landStatistikk.verdi shouldBe "6.4"
            landStatistikk.antallPersonerIBeregningen shouldBe 3365162

            val bransjeStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == "BRANSJE" }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalerIBeregningen.size shouldBe 4 // skal være ett år
            bransjeStatistikk.kvartalerIBeregningen.forAll { it.årstall shouldBe 2024 }
            bransjeStatistikk.verdi shouldBe "5.8"
            bransjeStatistikk.antallPersonerIBeregningen shouldBe 88563

            val virksomhetStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == "VIRKSOMHET" }
            virksomhetStatistikk.shouldNotBeNull()
            virksomhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeBarnehage.navn
        }
    }
}
