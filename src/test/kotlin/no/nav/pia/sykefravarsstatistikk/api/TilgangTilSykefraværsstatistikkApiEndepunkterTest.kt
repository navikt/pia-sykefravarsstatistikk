package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.assertions.shouldFailWithMessage
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enhetsregisteretContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.postgresContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetUtenTilgang
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetMedEnkelrettighetBransjeBarnehage
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetUtenTilgang
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import no.nav.pia.sykefravarsstatistikk.helper.withoutToken
import kotlin.test.BeforeTest
import kotlin.test.Test

class TilgangTilSykefraværsstatistikkApiEndepunkterTest {
    @BeforeTest
    fun cleanUp() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
            enhetsregisteretContainerHelper.slettAlleEnheterOgUnderenheter()
            postgresContainerHelper.slettAlleStatistikk()
        }
    }

    @Test
    fun `Bruker som når et ukjent endepunkt får '404 - Not found' i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${underenhetMedTilhørighetUtenBransje.somNæringsdrivende().orgnr}/sykefravarshistorikk/alt",
                config = withToken(),
            )
            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            enhetsregisteretContainerHelper.leggTilIEnhetsregisteret(
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje,
                underenhet = underenhetMedTilhørighetUtenBransje,
            )

            val response = TestContainerHelper.hentKvartalsvisStatistikkResponse(
                orgnr = underenhetMedTilhørighetUtenBransje.somNæringsdrivende().orgnr,
                config = withoutToken(),
            )

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Bruker som ikke er innlogget får en '401 - Unauthorized' i response (aggregert endepunkt)`() {
        runBlocking {
            enhetsregisteretContainerHelper.leggTilIEnhetsregisteret(
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje,
                underenhet = underenhetMedTilhørighetUtenBransje,
            )

            val response = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetMedTilhørighetUtenBransje.somNæringsdrivende().orgnr,
                config = withoutToken(),
            )

            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            enhetsregisteretContainerHelper.leggTilIEnhetsregisteret(
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje,
                underenhet = underenhetMedTilhørighetUtenBransje,
            )

            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 403 Forbidden, body: {\"message\":\"You don't have access to this resource\"}",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetMedTilhørighetUtenBransje.somNæringsdrivende().orgnr,
                    config = withToken(),
                )
            }
        }
    }

    @Test
    fun `Innlogget bruker uten tilgang til virksomhet får '403 - Forbidden' i response (aggregert endepunkt)`() {
        runBlocking {
            enhetsregisteretContainerHelper.leggTilIEnhetsregisteret(
                overordnetEnhet = overordnetEnhetUtenTilgang,
                underenhet = underenhetUtenTilgang,
            )

            val response = TestContainerHelper.hentAggregertStatistikkResponse(
                orgnr = underenhetUtenTilgang.somNæringsdrivende().orgnr,
                config = withToken(),
            )
            response.status shouldBe HttpStatusCode.Forbidden
        }
    }

    @Test
    fun `Innlogget bruker uten enkeltrettighet for sykefraværsstatistikk får '403 - Forbidden' i response (kvartalsvis endepunkt)`() {
        runBlocking {
            enhetsregisteretContainerHelper.leggTilIEnhetsregisteret(
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage,
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage,
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.somNæringsdrivende(),
                altinn2Rettighet = "enkeltrettighet_som_ikke_er_sykefraværsstatistikk",
            )
            kafkaContainerHelper.sendStatistikk(
                underenhet = underenhetMedEnkelrettighetBransjeBarnehage.somNæringsdrivende(),
                overordnetEnhet = overordnetEnhetMedEnkelrettighetBransjeBarnehage.somOverordnetEnhet(),
            )

            val expectedMessage = "{\"message\":\"You don't have access to this resource\"}"
            val response = TestContainerHelper.hentKvartalsvisStatistikkResponse(
                orgnr = underenhetMedEnkelrettighetBransjeBarnehage.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            response.status shouldBe HttpStatusCode.Forbidden
            response.bodyAsText() shouldBe expectedMessage
        }
    }
}
