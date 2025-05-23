package no.nav.pia.sykefravarsstatistikk

import io.ktor.server.application.Application
import io.ktor.server.engine.addShutdownHook
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.pia.sykefravarsstatistikk.api.aggregering.AggregertStatistikkService
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService
import no.nav.pia.sykefravarsstatistikk.api.auth.EnhetsregisteretService
import no.nav.pia.sykefravarsstatistikk.eksport.SykefraværsstatistikkEksportService
import no.nav.pia.sykefravarsstatistikk.eksport.SykefraværsstatistikkProducer
import no.nav.pia.sykefravarsstatistikk.eksport.VirksomhetMetadataEksportService
import no.nav.pia.sykefravarsstatistikk.eksport.VirksomhetMetadataProducer
import no.nav.pia.sykefravarsstatistikk.importering.PubliseringsdatoConsumer
import no.nav.pia.sykefravarsstatistikk.importering.SykefraværsstatistikkConsumer
import no.nav.pia.sykefravarsstatistikk.importering.SykefraværsstatistikkImportService
import no.nav.pia.sykefravarsstatistikk.importering.VirksomhetMetadataConsumer
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.ApplikasjonsHelse
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic
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

    val publiseringsdatoService = PubliseringsdatoService(
        publiseringsdatoRepository = publiseringsdatoRepository,
    )
    val sykefraværsstatistikkImportService = SykefraværsstatistikkImportService(
        sykefraværsstatistikkRepository = sykefraværsstatistikkRepository,
    )
    val statistikkLandProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_LAND,
    )
    val statistikkSektorProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_SEKTOR,
    )
    val statistikkNæringProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_NÆRING,
    )
    val statistikkBransjeProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_BRANSJE,
    )
    val statistikkNæringskodeProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_NÆRINGSKODE,
    )
    val statistikkVirksomhetProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_VIRKSOMHET,
    )
    val statistikkVirksomhetGradertProdusent = SykefraværsstatistikkProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_VIRKSOMHET_GRADERT,
    )
    val statistikkMetadataVirksomhetProdusent = VirksomhetMetadataProducer(
        kafka = naisEnvironment.kafka,
        topic = Topic.STATISTIKK_EKSPORT_METADATA_VIRKSOMHET,
    )
    val sykefraværsstatistikkEksportService = SykefraværsstatistikkEksportService(
        sykefraværsstatistikkRepository = sykefraværsstatistikkRepository,
        statistikkLandProdusent = statistikkLandProdusent,
        statistikkSektorProdusent = statistikkSektorProdusent,
        statistikkNæringProdusent = statistikkNæringProdusent,
        statistikkBransjeProdusent = statistikkBransjeProdusent,
        statistikkNæringskodeProdusent = statistikkNæringskodeProdusent,
        statistikkVirksomhetProdusent = statistikkVirksomhetProdusent,
        statistikkVirksomhetGradertProdusent = statistikkVirksomhetGradertProdusent,
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

    VirksomhetMetadataConsumer(
        metadataService = MetadataService(MetadataRepository(dataSource = dataSource)),
        virksomhetMetadataEksportService = VirksomhetMetadataEksportService(
            statistikkMetadataVirksomhetProdusent = statistikkMetadataVirksomhetProdusent,
        ),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    PubliseringsdatoConsumer(
        metadataService = MetadataService(MetadataRepository(dataSource = dataSource)),
        applikasjonsHelse = applikasjonsHelse,
    ).run()

    listOf(
        Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
        Topic.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
    ).forEach { topic ->
        SykefraværsstatistikkConsumer(
            topic = topic,
            sykefraværsstatistikkImportService = sykefraværsstatistikkImportService,
            sykefraværsstatistikkEksportService = sykefraværsstatistikkEksportService,
            applikasjonsHelse = applikasjonsHelse,
        ).run()
    }

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
