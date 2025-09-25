package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.assertions.shouldFail
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.OVERORDNET_ENHET
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.helper.KvartalsvisSykefraværshistorikkTestDto
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.Companion.bigDecimalShouldBe
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetIBransjeAnlegg
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetIBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetIBransjeSykehus
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetINæringProduksjonAvMatfisk
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetINæringSkogskjøtsel
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetINæringUtleieAvEiendom
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetUtenNæringskode
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeAnlegg
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeSykehus
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringProduksjonAvMatfisk
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringSkogskjøtsel
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtleieAvEiendom
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetMedOverordnetEnhetSomIkkeHarNæringskode
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import no.nav.pia.sykefravarsstatistikk.persistering.ImporttidspunktRepository.Companion.NÅVÆRENDE_KVARTAL
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

    /*
       Kvartalsvis statistikk
     * */
    @Test
    fun `Innlogget bruker får en 200`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                overordnetEnhet = overordnetEnhetINæringUtleieAvEiendom.somOverordnetEnhet(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetINæringUtleieAvEiendom.organisasjonsnummer,
                config = withToken(),
            ).shouldNotBeNull()
        }
    }

    @Test
    fun `Aggregert statistikk skal være maskert i response`() {
        // OBS: mangler   "prosentSiste4KvartalerKorttid", "prosentSiste4KvartalerLangtid",
        // "tapteDagsverkTotalt": og "muligeDagsverkTotalt"
        runBlocking {
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk(
                overordnetEnhetINæringUtleieAvEiendom.somOverordnetEnhet().sektor!!,
            )
            kafkaContainerHelper.sendNæringsstatistikk(
                næring = underenhetINæringUtleieAvEiendom.somNæringsdrivende().næringskode.næring,
            )
            kafkaContainerHelper.sendEnkelVirksomhetsstatistikk(
                virksomhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                årstall = 2024,
                harForFåAnsatte = true,
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val statistikkResponse = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            val statistikk = Json.decodeFromString<AggregertStatistikkResponseDto>(statistikkResponse.bodyAsText())
            val maskertStatistikkForVirksomhet =
                statistikk.prosentSiste4KvartalerTotalt.find { it.statistikkategori == VIRKSOMHET }
            maskertStatistikkForVirksomhet shouldBe null
            val statistikkForNæring =
                statistikk.prosentSiste4KvartalerTotalt.find { it.statistikkategori == NÆRING }
            statistikkForNæring shouldNotBe null
            val statistikkForLand =
                statistikk.prosentSiste4KvartalerTotalt.find { it.statistikkategori == LAND }
            statistikkForLand shouldNotBe null

            statistikk.tapteDagsverkTotalt shouldBe emptyList()
            statistikk.muligeDagsverkTotalt shouldBe emptyList()
        }
    }

    @Test
    fun `Aggregert statistikk skal også være maskert for Næring (eller Bransje) i response`() {
        runBlocking {
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk(
                sektor = overordnetEnhetINæringUtleieAvEiendom.somOverordnetEnhet().sektor!!,
            )
            kafkaContainerHelper.sendNæringsstatistikk(
                næring = underenhetINæringUtleieAvEiendom.somNæringsdrivende().næringskode.næring,
                harForFåAnsatte = true,
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val statistikk = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            val statistikkForNæring =
                statistikk.prosentSiste4KvartalerTotalt.find { it.statistikkategori == NÆRING }
            statistikkForNæring shouldBe null
            val trendINæring = statistikk.trendTotalt.find { it.statistikkategori == NÆRING }
            trendINæring shouldNotBe null
            trendINæring?.verdi shouldBe "0.0"
            trendINæring?.kvartalerIBeregningen shouldContainExactlyInAnyOrder listOf(
                NÅVÆRENDE_KVARTAL,
                NÅVÆRENDE_KVARTAL.minusEttÅr(),
            )
        }
    }

    @Test
    fun `Kvartalsvis statistikk skal være maskert i response`() {
        runBlocking {
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk(
                sektor = overordnetEnhetINæringUtleieAvEiendom.somOverordnetEnhet().sektor!!,
            )
            kafkaContainerHelper.sendNæringsstatistikk(
                næring = underenhetINæringUtleieAvEiendom.somNæringsdrivende().næringskode.næring,
            )
            kafkaContainerHelper.sendEnkelVirksomhetsstatistikk(
                virksomhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                årstall = 2020,
                harForFåAnsatte = true,
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val statistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            // prettyPrint(statistikk)
            val actualStatistikkForVirksomhet =
                statistikk.firstOrNull { it.type == VIRKSOMHET.name }
            val kvartalsvisSykefraværsprosent = actualStatistikkForVirksomhet?.kvartalsvisSykefraværsprosent
            kvartalsvisSykefraværsprosent shouldNotBe null
            kvartalsvisSykefraværsprosent?.forEach { sykefravarsstatistikk ->
                sykefravarsstatistikk.erMaskert shouldBe true
                sykefravarsstatistikk.prosent shouldBe null
                sykefravarsstatistikk.tapteDagsverk shouldBe null
                sykefravarsstatistikk.muligeDagsverk shouldBe null
            }
        }
    }

    @Test
    fun `Får IKKE feil ved manglende statistikk`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetIBransjeAnlegg.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            shouldFail {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetIBransjeAnlegg.organisasjonsnummer,
                    config = withToken(),
                )
            }
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk(overordnetEnhetIBransjeAnlegg.somOverordnetEnhet().sektor!!)

            val bransje = underenhetIBransjeAnlegg.somNæringsdrivende().bransje()
            if (bransje != null) {
                kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
            } else {
                kafkaContainerHelper.sendNæringsstatistikk(
                    næring = underenhetIBransjeAnlegg.somNæringsdrivende().næringskode.næring,
                )
            }

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetIBransjeAnlegg.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            kvartalsvisStatistikk.firstOrNull { it.type == LAND.name }!!.kvartalsvisSykefraværsprosent.size shouldBe 20
            kvartalsvisStatistikk.firstOrNull { it.type == SEKTOR.name }!!.kvartalsvisSykefraværsprosent.size shouldBe 20
            kvartalsvisStatistikk.firstOrNull {
                it.type == (
                    if (bransje !== null) {
                        BRANSJE.name
                    } else {
                        NÆRING.name
                    }
                )
            }!!.kvartalsvisSykefraværsprosent.size shouldBe 20
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet og enkelttilgang får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetINæringProduksjonAvMatfisk.somOverordnetEnhet(),
                underenhet = underenhetINæringProduksjonAvMatfisk.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringProduksjonAvMatfisk.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetINæringProduksjonAvMatfisk.somNæringsdrivende().orgnr,
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
            sektorStatistikk.label shouldBe
                overordnetEnhetINæringProduksjonAvMatfisk.somOverordnetEnhet().sektor?.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }.shouldBeNull()

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe
                underenhetINæringProduksjonAvMatfisk.somNæringsdrivende().næringskode.næring.navn
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetINæringProduksjonAvMatfisk.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetINæringProduksjonAvMatfisk.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkTestDto>()
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet (i bransje) og enkelttilgang får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetIBransjeSykehus.somOverordnetEnhet(),
                underenhet = underenhetIBransjeSykehus.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetIBransjeSykehus.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetIBransjeSykehus.somNæringsdrivende().orgnr,
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

            val bransje = underenhetIBransjeSykehus.somNæringsdrivende().bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetIBransjeSykehus.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetIBransjeSykehus.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkTestDto>()
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet, enkelttilgang og overordnet enhet får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetINæringSkogskjøtsel.somOverordnetEnhet(),
                underenhet = underenhetINæringSkogskjøtsel.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringSkogskjøtsel.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetINæringSkogskjøtsel.somNæringsdrivende().orgnr,
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
            sektorStatistikk.label shouldBe
                overordnetEnhetINæringSkogskjøtsel.somOverordnetEnhet().sektor?.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }.shouldBeNull()

            val næringsStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }
            næringsStatistikk.shouldNotBeNull()
            næringsStatistikk.label shouldBe
                underenhetINæringSkogskjøtsel.somNæringsdrivende().næringskode.næring.navn
            næringsStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.9
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 1239902.548524
            næringsStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 73154.250363

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetINæringSkogskjøtsel.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetINæringSkogskjøtsel.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkTestDto>()
        }
    }

    @Test
    fun `Bruker med tilhørighet til virksomhet (i bransje), enkelttilgang og overordnet enhet får kvartalsvis statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetIBransjeBarnehage.somOverordnetEnhet(),
                underenhet = underenhetIBransjeBarnehage.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                overordnetEnhet = overordnetEnhetIBransjeBarnehage.somOverordnetEnhet(),
                underenhet = underenhetIBransjeBarnehage.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val kvartalsvisStatistikk = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetIBransjeBarnehage.somNæringsdrivende().orgnr,
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
            sektorStatistikk.label shouldBe
                overordnetEnhetIBransjeBarnehage.somOverordnetEnhet().sektor?.beskrivelse
            sektorStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 6.3
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 1275292.330000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 19790049.740000

            kvartalsvisStatistikk.firstOrNull { it.type == NÆRING.name }.shouldBeNull()

            val bransje = underenhetIBransjeBarnehage.somNæringsdrivende().bransje()!!
            val bransjeStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == BRANSJE.name }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalsvisSykefraværsprosent.size shouldBe 20 // Skal være 5 år
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 5.8
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 270744.659570
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 4668011.371895

            val underenhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == VIRKSOMHET.name }
            underenhetStatistikk.shouldNotBeNull()
            underenhetStatistikk.label shouldBe underenhetIBransjeBarnehage.navn
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk bigDecimalShouldBe 154.5439
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().prosent bigDecimalShouldBe 28.3
            underenhetStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk bigDecimalShouldBe 761.3

            val overordnetEnhetStatistikk = kvartalsvisStatistikk.firstOrNull { it.type == OVERORDNET_ENHET.name }
            overordnetEnhetStatistikk.shouldNotBeNull()
            overordnetEnhetStatistikk.label shouldBe overordnetEnhetIBransjeBarnehage.navn
            overordnetEnhetStatistikk.kvartalsvisSykefraværsprosent shouldBe emptyList<KvartalsvisSykefraværshistorikkTestDto>()
        }
    }

    /*
      Aggregert statistikk
     * */
    @Test
    fun `Bruker får aggregert statistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetIBransjeBarnehage.somOverordnetEnhet(),
                underenhet = underenhetIBransjeBarnehage.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetIBransjeBarnehage.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val aggregertStatistikkDto = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhetIBransjeBarnehage.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            val bransje = underenhetIBransjeBarnehage.somNæringsdrivende().bransje()!!

            val landStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == LAND }
            landStatistikk.shouldNotBeNull()
            landStatistikk.label shouldBe "Norge"
            landStatistikk.kvartalerIBeregningen.size shouldBe 4 // skal være ett år
            landStatistikk.kvartalerIBeregningen.shouldContainExactlyInAnyOrder(NÅVÆRENDE_KVARTAL.sisteFireKvartaler())
            // TODO: finn en mer robust måte å teste hvilke kvartaler som er med i beregning, sjekk over flere år?
            landStatistikk.verdi shouldBe "6.4"
            landStatistikk.antallPersonerIBeregningen shouldBe 3365162

            aggregertStatistikkDto.prosentSiste4KvartalerTotalt.firstOrNull { it.statistikkategori == NÆRING }
                .shouldBeNull()

            val bransjeStatistikk =
                aggregertStatistikkDto.prosentSiste4KvartalerTotalt.firstOrNull { it.statistikkategori == BRANSJE }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn
            bransjeStatistikk.kvartalerIBeregningen.size shouldBe 4 // skal være ett år
            bransjeStatistikk.kvartalerIBeregningen.shouldContainExactlyInAnyOrder(NÅVÆRENDE_KVARTAL.sisteFireKvartaler())
            bransjeStatistikk.verdi shouldBe "5.8"
            bransjeStatistikk.antallPersonerIBeregningen shouldBe 88563

            val virksomhetStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == VIRKSOMHET }
            virksomhetStatistikk.shouldNotBeNull()
            virksomhetStatistikk.label shouldBe underenhetIBransjeBarnehage.navn
        }
    }

    @Test
    fun `Håndterer manglende næringskode på overordnet enhet fra brreg`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetUtenNæringskode.somOverordnetEnhet(),
                underenhet = underenhetMedOverordnetEnhetSomIkkeHarNæringskode.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedOverordnetEnhetSomIkkeHarNæringskode.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val result = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhetMedOverordnetEnhetSomIkkeHarNæringskode.somNæringsdrivende().orgnr,
                config = withToken(),
            )
            result.shouldNotBeNull()
        }
    }
}
