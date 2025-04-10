package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.NÆRING
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode
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

    /*
       Data fra enhetsregisteret kan mangle noe informasjon, som for eksempel institusjonell sektor kode.
     * */
    @Test
    fun `Virksomhet uten institusjonell sektor kode fungerer både for aggregert og kvartalsvis`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode,
                underenhet = underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode,
            )

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2,
                altinn3Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
            )

            val aggregertStatistikkDto = TestContainerHelper.hentAggregertStatistikk(
                orgnr = underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode.orgnr,
                config = withToken(),
            )

            val bransje = underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode.bransje()!!

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
            virksomhetStatistikk.label shouldBe underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode.navn

            val kvartalsvisStatistikkDto = TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedTilhørighetBransjeByggUtenInstitusjonellSektorKode.orgnr,
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
}
