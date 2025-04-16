package no.nav.pia.sykefravarsstatistikk.api.auditlog

import io.kotest.assertions.shouldFailWithMessage
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.helper.AuthContainerHelper.Companion.FNR
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.shouldContainLog
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.overordnetEnhetINæringUtleieAvEiendom
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somNæringsdrivende
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.somOverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtleieAvEiendom
import no.nav.pia.sykefravarsstatistikk.helper.TestdataHelper.Companion.underenhetINæringUtvinningAvRåoljeOgGass
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
                overordnetEnhet = overordnetEnhetINæringUtleieAvEiendom.somOverordnetEnhet(),
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
            )
            altinnTilgangerContainerHelper.leggTilRettigheter(
                underenhet = underenhetINæringUtleieAvEiendom.somNæringsdrivende(),
                altinn2Rettighet = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2,
            )

            TestContainerHelper.hentKvartalsvisStatistikk(
                orgnr = underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr,
                config = withToken(),
            )

            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog\\|1.0\\|audit:access\\|Sporingslogg\\|INFO".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har utført følgende kall mot organisajonsnummer ${underenhetINæringUtleieAvEiendom.somNæringsdrivende().orgnr} path: ".toRegex()
        }
    }

    @Test
    fun `auditlogger feil ved manglende rettigheter`() {
        runBlocking {
            shouldFailWithMessage(
                "Feil ved henting av kvartalsvis statistikk, status: 403 Forbidden, body: {\"message\":\"You don't have access to this resource\"}",
            ) {
                TestContainerHelper.hentKvartalsvisStatistikk(
                    orgnr = underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr,
                    config = withToken(),
                )
            }

            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har ikke tilgang til organisasjonsnummer ${underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr}".toRegex()
            applikasjon shouldContainLog
                "requestMethod=GET request=/sykefravarsstatistikk/${underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr}/historikk/kvartalsvis".toRegex()
            applikasjon shouldContainLog
                "CEF:0|pia-sykefravarsstatistikk|auditLog|1.0|audit:access|Sporingslogg|INFO|end=".toRegex()
        }
    }

    @Test
    fun `auditlogger feil ved manglende rettigheter i kall mot aggregert endepunkt`() {
        runBlocking {
            shouldFailWithMessage(
                "Feil ved henting av aggregert statistikk, status: 403 Forbidden, body: {\"message\":\"You don't have access to this resource\"}",
            ) {
                TestContainerHelper.hentAggregertStatistikk(
                    orgnr = underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr,
                    config = withToken(),
                )
            }

            applikasjon shouldContainLog "CEF:0\\|pia-sykefravarsstatistikk\\|auditLog".toRegex()
            applikasjon shouldContainLog
                "msg=$FNR har ikke tilgang til organisasjonsnummer ${underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr}".toRegex()
            applikasjon shouldContainLog
                "requestMethod=GET request=/sykefravarsstatistikk/${underenhetINæringUtvinningAvRåoljeOgGass.somNæringsdrivende().orgnr}/siste4kvartaler/aggregert".toRegex()
            applikasjon shouldContainLog
                "CEF:0|pia-sykefravarsstatistikk|auditLog|1.0|audit:access|Sporingslogg|INFO|end=".toRegex()
        }
    }
}
