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
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.overordnetEnhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.underenhetUtenTilgang
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
                url = "/${underenhetMedTilhørighetUtenBransje.orgnr}/sykefravarshistorikk/alt",
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
                orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                config = withoutToken(),
            )

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response (aggregert endepunkt)`() {
        runBlocking {
            val response = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
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
                    orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                    config = withToken(),
                )
            }
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response (aggregert endepunkt)`() {
        runBlocking {
            val response = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetUtenTilgang.orgnr,
                config = withToken(),
            )
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    @Test
    fun `Innlogget bruker uten enkelrettighet til virksomhet får '403 - Forbidden' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.orgnr,
                altinn2Rettighet = "ingen_tilgang_til_statistikk",
            )
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage,
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage,
            )

            val expectedMessage = "{\"message\":\"You don't have access to this resource\"}"
            val response = TestContainerHelper.hentKvartalsvisStatistikkResponse(
                orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                config = withToken(),
            )

            response.status shouldBe HttpStatusCode.Forbidden
            response.bodyAsText() shouldBe expectedMessage
        }
    }
}
