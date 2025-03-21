package no.nav.pia.sykefravarsstatistikk.api.auditlog

import io.kotest.assertions.shouldFailWithMessage
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.helper.AuthContainerHelper.Companion.FNR
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetUtenTilgang
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetMedTilhørighetUtenBransje
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.BeforeTest
import kotlin.test.Test

class AuditlogTest {
    @BeforeTest
    fun cleanUp() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
        }
    }

    @Test
    fun `auditlogger autorisert uthenting av kvartalsvis sykefraværsstatistikk`() {
        runBlocking {
            kafkaContainerHelper.sendStatistikk(
                overordnetEnhet = overordnetEnhetMedTilhørighetUtenBransje,
                underenhet = underenhetMedTilhørighetUtenBransje,
            )

            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetMedTilhørighetUtenBransje.orgnr,
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
            )

            TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetMedTilhørighetUtenBransje.orgnr,
                config = withToken(),
            )

            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog\\|1.0\\|audit:access\\|Sporingslogg\\|INFO".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har utført følgende kall mot organisajonsnummer ${underenhetMedTilhørighetUtenBransje.orgnr} path: ".toRegex()
        }
    }

    @Test
    fun `auditlogger feil ved manglende rettigheter`() {
        runBlocking {
            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 403 Forbidden, body: {\"message\":\"You don't have access to this resource\"}",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetUtenTilgang.orgnr,
                    config = withToken(),
                )
            }

            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har ikke tilgang til organisasjonsnummer ${underenhetUtenTilgang.orgnr}".toRegex()
        }
    }
}
