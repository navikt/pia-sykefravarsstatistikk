package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.assertions.shouldFailWithMessage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetIBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetIBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtleieAvEiendom
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtvinningAvRåoljeOgGass
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import no.nav.pia.sykefravarsstatistikk.helper.withoutToken
import kotlin.test.BeforeTest
import kotlin.test.Test

class TilgangTilSykefraværsstatistikkApiEndepunkterTest {
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
                url = "/${underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr}/sykefravarshistorikk/alt",
                config = withToken(),
            )
            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            val response = TestContainerHelper.hentKvartalsvisStatistikkResponse(
                orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                config = withoutToken(),
            )

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response (aggregert endepunkt)`() {
        runBlocking {
            val response = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                config = withoutToken(),
            )

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 403 Forbidden, body: {\"message\":\"You don't have access to this resource\"}",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                    config = withToken(),
                )
            }
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response (aggregert endepunkt)`() {
        runBlocking {
            val response = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr,
                config = withToken(),
            )
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    @Test
    fun `Innlogget bruker uten enkeltrettighet for sykefraværsstatistikk får '403 - Forbidden' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetIBransjeBarnehage.somNæringsdrivende(),
                altinn2Rettighet = "enkeltrettighet_som_ikke_er_sykefraværsstatistikk",
            )
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetIBransjeBarnehage.somNæringsdrivende(),
                overordnetEnhet = overordnetEnhetIBransjeBarnehage.somOverordnetEnhet(),
            )

            val expectedMessage = "{\"message\":\"You don't have access to this resource\"}"
            val response = TestContainerHelper.hentKvartalsvisStatistikkResponse(
                orgnr = underenhetIBransjeBarnehage.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            response.status shouldBe HttpStatusCode.Forbidden
            response.bodyAsText() shouldBe expectedMessage
        }
    }
}
