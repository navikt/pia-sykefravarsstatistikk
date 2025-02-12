package no.nav.pia.sykefravarsstatistikk

import io.ktor.server.application.Application
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import no.nav.pia.sykefravarsstatistikk.importering.PubliseringsdatoConsumer
import no.nav.pia.sykefravarsstatistikk.importering.SykefraværsstatistikkConsumer
import no.nav.pia.sykefravarsstatistikk.importering.VirksomhetMetadataConsumer
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.ApplikasjonsHelse
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureMonitoring
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureRouting
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureSerialization
import no.nav.pia.sykefravarsstatistikk.persistering.MetadataRepository
import no.nav.pia.sykefravarsstatistikk.persistering.MetadataService
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService
import java.util.concurrent.TimeUnit

fun main() {
    val naisEnvironment = NaisEnvironment()
    val applikasjonsHelse = ApplikasjonsHelse()
    val dataSource = createDataSource(database = naisEnvironment.database)
    runMigration(dataSource = dataSource)

    val sykefraværsstatistikkService =
        SykefraværsstatistikkService(sykefraværsstatistikkRepository = SykefraværsstatistikkRepository(dataSource = dataSource))

    SykefraværsstatistikkConsumer(
        topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        sykefraværsstatistikkService = SykefraværsstatistikkService(SykefraværsstatistikkRepository(dataSource = dataSource)),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    SykefraværsstatistikkConsumer(
        topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        sykefraværsstatistikkService = SykefraværsstatistikkService(SykefraværsstatistikkRepository(dataSource = dataSource)),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    VirksomhetMetadataConsumer(
        topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA,
        metadataService = MetadataService(MetadataRepository(dataSource = dataSource)),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    PubliseringsdatoConsumer(
        topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_PUBLISERINGSDATO,
        metadataService = MetadataService(MetadataRepository(dataSource = dataSource)),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configure(
            sykefraværsstatistikkService = sykefraværsstatistikkService,
        )
    }.also {
        // https://doc.nais.io/nais-application/good-practices/#handles-termination-gracefully
        it.addShutdownHook {
            it.stop(3, 5, TimeUnit.SECONDS)
        }
    }.start(wait = true)
}

fun Application.configure(sykefraværsstatistikkService: SykefraværsstatistikkService) {
    configureMonitoring()
    configureSerialization()
    configureRouting(sykefraværsstatistikkService = sykefraværsstatistikkService)
}
