package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.assertions.shouldFail
import io.kotest.assertions.shouldFailWithMessage
import io.kotest.inspectors.forAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.OVERORDNET_ENHET
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetUtenStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedEnkelrettighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetBransjeSykehus
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedTilhørighetUtenBransje2
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetUtenStatistikk
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetBransjeSykehus
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetUtenBransje2
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.BeforeTest
import kotlin.test.Test

class SykefraværsstatistikkApiEndepunkterTest {
    @BeforeTest
    fun cleanUp() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
            postgresContainerHelper.slettAlleStatistikk()
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
    fun `Innlogget bruker uten enkelrettighet til virksomhet får '403 - Forbidden' i response`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                altinn2Rettighet = "ingen_tilgang_til_statistikk",
            )
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage,
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage,
            )

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
    fun `Får IKKE feil ved manglende statistikk`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = enUnderenhetUtenStatistikk.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            shouldFail {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = enUnderenhetUtenStatistikk.orgnr,
                    config = withToken(),
                )
            }
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk(overordnetEnhetUtenStatistikk.sektor)

            val bransje = enUnderenhetUtenStatistikk.bransje()
            if (bransje != null) {
                kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
            } else {
                kafkaContainerHelper.sendNæringsstatistikk(næring = enUnderenhetUtenStatistikk.næringskode.næring)
            }

            TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = enUnderenhetUtenStatistikk.orgnr,
                config = withToken(),
            )
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

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == LAND.name }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == SEKTOR.name }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedTilhørighetUtenBransje2.sektor.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }.shouldBeNull()

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje.næringskode.næring.tosifferIdentifikator
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedTilhørighetUtenBransje2.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkDto>()
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

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == LAND.name }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == SEKTOR.name }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe Sektor.STATLIG.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }.shouldBeNull()

            val bransje = underenhetMedEnkelrettighetBransjeSykehus.bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeSykehus.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedTilhørighetBransjeSykehus.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkDto>()
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

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == LAND.name }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == SEKTOR.name }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetUtenBransje.sektor.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }.shouldBeNull()

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje2.næringskode.næring.tosifferIdentifikator
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetUtenBransje2.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetUtenBransje.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkDto>()
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

            val landStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == LAND.name }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.4
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 11539578.440000
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 180204407.260000

            val sektorStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == SEKTOR.name }
            sektorStatistikk.shouldNotBeNull()
            sektorStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetBransjeBarnehage.sektor.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }.shouldBeNull()

            val bransje = underenhetMedEnkelrettighetBransjeBarnehage.bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeBarnehage.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetMedEnkelrettighetBransjeBarnehage.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkDto>()
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
                .firstOrNull { it.statistikkategori == LAND.name }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalerIBeregningen.size shouldBe 4 // skal være ett år
            landStatistikk.kvartalerIBeregningen.forAll { it.årstall shouldBe 2024 }
            // TODO: finn en mer robust måte å teste hvilke kvartaler som er med i beregning, sjekk over flere år?
            landStatistikk.verdi shouldBe "6.4"
            landStatistikk.antallPersonerIBeregningen shouldBe 3365162

            aggregertStatistikkDto.prosentSiste4KvartalerTotalt.firstOrNull { it.statistikkategori == NÆRING.name }
                .shouldBeNull()

            val bransjeStatistikk =
                aggregertStatistikkDto.prosentSiste4KvartalerTotalt.firstOrNull { it.statistikkategori == BRANSJE.name }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalerIBeregningen.size shouldBe 4 // skal være ett år
            bransjeStatistikk.kvartalerIBeregningen.forAll { it.årstall shouldBe 2024 }
            bransjeStatistikk.verdi shouldBe "5.8"
            bransjeStatistikk.antallPersonerIBeregningen shouldBe 88563

            val virksomhetStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == VIRKSOMHET.name }
            virksomhetStatistikk.shouldNotBeNull()
            virksomhetStatistikk.label shouldBe underenhetMedEnkelrettighetBransjeBarnehage.navn
        }
    }
}
