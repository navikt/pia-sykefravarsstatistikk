package no.nav.pia.sykefravarsstatistikk

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.pia.sykefravarsstatistikk.importering.SykefraværsstatistikkØvrigeKategorierConsumer
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.ApplikasjonsHelse
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureMonitoring
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureRouting
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureSerialization
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService

fun main() {
    val naisEnvironment = NaisEnvironment()
    val applikasjonsHelse = ApplikasjonsHelse()
    val dataSource = createDataSource(database = naisEnvironment.database)
    runMigration(dataSource = dataSource)

    SykefraværsstatistikkØvrigeKategorierConsumer(
        sykefraværsstatistikkService = SykefraværsstatistikkService(SykefraværsstatistikkRepository(dataSource = dataSource)),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::configure).start(wait = true)
}

fun Application.configure() {
    configureMonitoring()
    configureSerialization()
    configureRouting()
}
