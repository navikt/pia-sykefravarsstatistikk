package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.helper.KvartalsvisSykefraværshistorikkTestDto
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetIBransjeByggUtenInstitusjonellSektorKode
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetMedUnderenhetUtenNæringskode
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somIkkeNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeByggUtenInstitusjonellSektorKode
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetUtenNæringskode
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import no.nav.pia.sykefravarsstatistikk.persistering.INGEN_SEKTOR_LABEL
import kotlin.test.BeforeTest
import kotlin.test.Test

class EdgeCasesSykefraværsstatistikkApiEndepunkterTest {
    @BeforeTest
    fun cleanUp() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
            postgresContainerHelper.slettAlleStatistikk()
        }
    }

    @Test
    fun `Håndtere virksomhet som ikke finnes i enhetsregisteret`() {
        val orgnrTilEnVirksomhetSomIkkeFinnesIBrreg = "123456789"
        runBlocking {
            val aggregertStatistikkResponse = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = orgnrTilEnVirksomhetSomIkkeFinnesIBrreg,
                config = withToken(),
            )

            aggregertStatistikkResponse.status shouldBe HttpStatusCode.NotFound
        }
    }

    /*
       Data fra enhetsregisteret kan mangle noe informasjon, som for eksempel institusjonell sektor kode.
     * */
    @Test
    fun `Virksomhet uten institusjonell sektor kode fungerer både for aggregert og kvartalsvis`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetIBransjeByggUtenInstitusjonellSektorKode.somOverordnetEnhet(),
                underenhet = underenhetIBransjeByggUtenInstitusjonellSektorKode.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetIBransjeByggUtenInstitusjonellSektorKode.somNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val aggregertStatistikkDto = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhetIBransjeByggUtenInstitusjonellSektorKode.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            val bransje = underenhetIBransjeByggUtenInstitusjonellSektorKode.somNæringsdrivende().bransje()!!

            val landStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == LAND }
            landStatistikk.shouldNotBeNull()
            aggregertStatistikkDto.prosentSiste4KvartalerTotalt.firstOrNull { it.statistikkategori == NÆRING }
                .shouldBeNull()
            val bransjeStatistikk =
                aggregertStatistikkDto.prosentSiste4KvartalerTotalt.firstOrNull { it.statistikkategori == BRANSJE }
            bransjeStatistikk.shouldNotBeNull()
            bransjeStatistikk.label shouldBe bransje.navn

            val virksomhetStatistikk = aggregertStatistikkDto.prosentSiste4KvartalerTotalt
                .firstOrNull { it.statistikkategori == VIRKSOMHET }
            virksomhetStatistikk.shouldNotBeNull()
            virksomhetStatistikk.label shouldBe underenhetIBransjeByggUtenInstitusjonellSektorKode.navn

            val kvartalsvisStatistikkDto = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetIBransjeByggUtenInstitusjonellSektorKode.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            kvartalsvisStatistikkDto.shouldNotBeNull()
            val actualStatistikkForSektor =
                kvartalsvisStatistikkDto.firstOrNull { it.type == SEKTOR.name }

            actualStatistikkForSektor.shouldNotBeNull()
            actualStatistikkForSektor.label shouldBe INGEN_SEKTOR_LABEL
            actualStatistikkForSektor.kvartalsvisSykefraværsprosent shouldBe emptyList()
        }
    }

    @Test
    fun `Virksomhet uten næringskode fungerer (dvs ikke krasjer med 500) både for aggregert og kvartalsvis`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedUnderenhetUtenNæringskode.somOverordnetEnhet(),
                underenhet = underenhetUtenNæringskode.somIkkeNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetUtenNæringskode.somIkkeNæringsdrivende(),
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val aggregertStatistikkResponse = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetUtenNæringskode.somIkkeNæringsdrivende().orgnr,
                config = withToken(),
            )
            aggregertStatistikkResponse.status shouldBe HttpStatusCode.BadRequest

            val kvartalsvisStatistikkResponse = TestContainerHelper.hentKvartalsvisStatistikkResponse(
                orgnr = underenhetUtenNæringskode.somIkkeNæringsdrivende().orgnr,
                config = withToken(),
            )

            kvartalsvisStatistikkResponse.status shouldBe HttpStatusCode.OK
            val kvartalsvisStatistikkDto: List<KvartalsvisSykefraværshistorikkTestDto> =
                kvartalsvisStatistikkResponse.body()
            kvartalsvisStatistikkDto shouldBe emptyList()
        }
    }
}
