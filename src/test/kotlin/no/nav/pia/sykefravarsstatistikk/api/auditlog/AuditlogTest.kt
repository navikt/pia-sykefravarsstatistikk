package no.nav.pia.sykefravarsstatistikk.api.auditlog

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.helper.AuthContainerHelper.Companion.FNR
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enVirksomhetUtenTilgangIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.Test

class AuditlogTest {
    @Test
    fun `auditlogger autorisert uthenting av kvartalsvis sykefraværsstatistikk`() {
        runBlocking {
            val bransje = "Sykehjem"
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
            kafkaContainerHelper.sendVirksomhetsstatistikk()

            val resultat =
                applikasjon.performGet(
                    url = "/${enUnderenhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                    config = withToken(),
                )

            resultat?.status shouldBe HttpStatusCode.OK
            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog\\|1.0\\|audit:access\\|Sporingslogg\\|INFO".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har utført følgende kall mot organisajonsnummer ${enUnderenhetIAltinn.orgnr} path: ".toRegex()
        }
    }

    @Test
    fun `auditlogger feil ved manglende rettigheter`() {
        runBlocking {
            val resultat = applikasjon.performGet(
                url = "/${enVirksomhetUtenTilgangIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            resultat?.status shouldBe HttpStatusCode.Forbidden
            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har ikke tilgang til organisasjonsnummer ${enVirksomhetUtenTilgangIAltinn.orgnr}".toRegex()
        }
    }
}
