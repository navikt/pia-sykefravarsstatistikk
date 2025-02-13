package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.helper.AltinnMockHelper.Companion.enVirksomhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.Test

class SykefraværsstatistikkApiEndepunkterTest {
    val orgnr = enVirksomhetIAltinn.orgnr

    @Test
    fun `Bruker som når et ukjent endepunkt får '404 - Not found' returncode i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/$orgnr/sykefravarshistorikk/alt",
            )
            resultat?.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Bruker som ikke er innlogget burde få en '401 - Unauthorized' returncode i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/$orgnr/sykefravarshistorikk/kvartalsvis",
            )
            resultat?.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Innlogget bruker får en 200`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/$orgnr/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )
            resultat?.status shouldBe HttpStatusCode.OK
            resultat?.bodyAsText() shouldBe "[]"
        }
    }
}
