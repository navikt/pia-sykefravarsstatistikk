package no.nav.pia.sykefravarsstatistikk

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.pia.sykefravarsstatistikk.api.aggregering.AggregertStatistikkService
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService
import no.nav.pia.sykefravarsstatistikk.api.auth.EnhetsregisteretService
import no.nav.pia.sykefravarsstatistikk.importering.PubliseringsdatoConsumer
import no.nav.pia.sykefravarsstatistikk.importering.SykefraværsstatistikkConsumer
import no.nav.pia.sykefravarsstatistikk.importering.VirksomhetMetadataConsumer
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.ApplikasjonsHelse
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureMonitoring
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureRouting
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins.configureSerialization
import no.nav.pia.sykefravarsstatistikk.persistering.ImporttidspunktRepository
import no.nav.pia.sykefravarsstatistikk.persistering.KvartalsvisSykefraværshistorikkService
import no.nav.pia.sykefravarsstatistikk.persistering.MetadataRepository
import no.nav.pia.sykefravarsstatistikk.persistering.MetadataService
import no.nav.pia.sykefravarsstatistikk.persistering.PubliseringsdatoService
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkGraderingRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkMedVarighetRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService
import java.util.concurrent.TimeUnit

fun main() {
    val naisEnvironment = NaisEnvironment()
    val altinnTilgangerService = AltinnTilgangerService()
    val applikasjonsHelse = ApplikasjonsHelse()
    val dataSource = createDataSource(database = naisEnvironment.database)
    runMigration(dataSource = dataSource)

    val sykefraværsstatistikkRepository = SykefraværsstatistikkRepository(dataSource = dataSource)
    val sykefraværsstatistikkMedVarighetRepository = SykefraværsstatistikkMedVarighetRepository(dataSource = dataSource)
    val sykefraværsstatistikkGraderingRepository = SykefraværsstatistikkGraderingRepository(dataSource = dataSource)
    val importtidspunktRepository = ImporttidspunktRepository(dataSource = dataSource)
    val publiseringsdatoRepository = MetadataRepository(dataSource = dataSource)
    val sykefraværsstatistikkService = SykefraværsstatistikkService(
        sykefraværsstatistikkRepository = sykefraværsstatistikkRepository,
    )

    val kvartalsvisSykefraværshistorikkService = KvartalsvisSykefraværshistorikkService(
        importtidspunktRepository = importtidspunktRepository,
        sykefraværsstatistikkRepository = sykefraværsstatistikkRepository,
    )

    val aggregertStatistikkService = AggregertStatistikkService(
        importtidspunktRepository = importtidspunktRepository,
        sykefraværsstatistikkRepository = sykefraværsstatistikkRepository,
        sykefraværsstatistikkMedVarighetRepository = sykefraværsstatistikkMedVarighetRepository,
        sykefraværsstatistikkGraderingRepository = sykefraværsstatistikkGraderingRepository,
    )

    val publiseringsdatoService = PubliseringsdatoService(publiseringsdatoRepository = publiseringsdatoRepository)

    SykefraværsstatistikkConsumer(
        topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        sykefraværsstatistikkService = sykefraværsstatistikkService,
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    SykefraværsstatistikkConsumer(
        topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
        sykefraværsstatistikkService = sykefraværsstatistikkService,
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

    val enhetsregisteretService = EnhetsregisteretService()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configure(
            altinnTilgangerService = altinnTilgangerService,
            aggregertStatistikkService = aggregertStatistikkService,
            kvartalsvisSykefraværshistorikkService = kvartalsvisSykefraværshistorikkService,
            enhetsregisteretService = enhetsregisteretService,
            publiseringsdatoService = publiseringsdatoService,
        )
    }.also {
        // https://doc.nais.io/nais-application/good-practices/#handles-termination-gracefully
        it.addShutdownHook {
            it.stop(3, 5, TimeUnit.SECONDS)
        }
    }.start(wait = true)
}

fun Application.configure(
    altinnTilgangerService: AltinnTilgangerService,
    aggregertStatistikkService: AggregertStatistikkService,
    kvartalsvisSykefraværshistorikkService: KvartalsvisSykefraværshistorikkService,
    enhetsregisteretService: EnhetsregisteretService,
    publiseringsdatoService: PubliseringsdatoService,
) {
    configureMonitoring()
    configureSerialization()
    configureRouting(
        altinnTilgangerService = altinnTilgangerService,
        aggregertStatistikkService = aggregertStatistikkService,
        kvartalsvisSykefraværshistorikkService = kvartalsvisSykefraværshistorikkService,
        enhetsregisteretService = enhetsregisteretService,
        publiseringsdatoService = publiseringsdatoService,
    )
}
