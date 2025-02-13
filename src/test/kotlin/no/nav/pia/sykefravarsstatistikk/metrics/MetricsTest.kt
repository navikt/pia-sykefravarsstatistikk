package no.nav.pia.sykefravarsstatistikk.metrics

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.applikasjon
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import kotlin.test.Test

class MetricsTest {
    @Test
    fun `skal servere metrikker på metrics endepunkt`() {
        runBlocking {
            val metrikkRespons = applikasjon.performGet("/metrics")
            metrikkRespons?.status shouldBe HttpStatusCode.OK
            metrikkRespons?.bodyAsText() shouldContain "jvm_threads_daemon_threads"
        }
    }
}
