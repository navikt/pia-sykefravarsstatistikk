package no.nav.pia.sykefravarsstatistikk.helper

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.time.withTimeoutOrNull
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.enUnderenhetIAltinn
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaConfig
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.util.TimeZone

class KafkaContainerHelper(
    network: Network = Network.newNetwork(),
    log: Logger = LoggerFactory.getLogger(KafkaContainerHelper::class.java),
) {
    private val kafkaNetworkAlias = "kafkaContainer"
    private var adminClient: AdminClient
    private var kafkaProducer: KafkaProducer<String, String>

    val kafkaContainer = ConfluentKafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.3"),
    )
        .withNetwork(network)
        .withNetworkAliases(kafkaNetworkAlias)
        .withLogConsumer(Slf4jLogConsumer(log).withPrefix(kafkaNetworkAlias).withSeparateOutputStreams())
        .withEnv(
            mapOf(
                "KAFKA_LOG4J_LOGGERS" to "org.apache.kafka.image.loader.MetadataLoader=WARN",
                "KAFKA_AUTO_LEADER_REBALANCE_ENABLE" to "false",
                "KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS" to "1",
                "TZ" to TimeZone.getDefault().id,
            ),
        )
        .withCreateContainerCmdModifier { cmd -> cmd.withName("$kafkaNetworkAlias-${System.currentTimeMillis()}") }
        .waitingFor(HostPortWaitStrategy())
        .apply {
            start()
            adminClient = AdminClient.create(mapOf(BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers))
            createTopics()
            kafkaProducer = producer()
        }

    // TODO: tas i bruk når vi sender kafka meldinger til andre applikasjoner
    fun nyKonsument(topic: KafkaTopics) =
        KafkaConfig(
            brokers = kafkaContainer.bootstrapServers,
            truststoreLocation = "",
            keystoreLocation = "",
            credstorePassword = "",
        )
            .consumerProperties(konsumentGruppe = topic.konsumentGruppe)
            .let { config ->
                KafkaConsumer(config, StringDeserializer(), StringDeserializer())
            }

    // TODO: tas i bruk når vi sender kafka meldinger til andre applikasjoner
    suspend fun ventOgKonsumerKafkaMeldinger(
        key: String,
        konsument: KafkaConsumer<String, String>,
        block: (meldinger: List<String>) -> Unit,
    ) {
        withTimeout(Duration.ofSeconds(5)) {
            launch {
                while (this.isActive) {
                    val records = konsument.poll(Duration.ofMillis(50))
                    val meldinger = records
                        .filter { it.key() == key }
                        .map { it.value() }
                    if (meldinger.isNotEmpty()) {
                        block(meldinger)
                        break
                    }
                }
            }
        }
    }

    fun envVars() =
        mapOf(
            "KAFKA_BROKERS" to "BROKER://$kafkaNetworkAlias:9093,PLAINTEXT://$kafkaNetworkAlias:9093",
            "KAFKA_TRUSTSTORE_PATH" to "",
            "KAFKA_KEYSTORE_PATH" to "",
            "KAFKA_CREDSTORE_PASSWORD" to "",
        )

    private fun createTopics() {
        adminClient.createTopics(
            listOf(
                NewTopic(KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER.navn, 1, 1.toShort()),
                NewTopic(KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET.navn, 1, 1.toShort()),
            ),
        )
    }

    private fun ConfluentKafkaContainer.producer(): KafkaProducer<String, String> =
        KafkaProducer(
            mapOf(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to this.bootstrapServers,
                CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "PLAINTEXT",
                ProducerConfig.ACKS_CONFIG to "1",
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION to "5",
                ProducerConfig.LINGER_MS_CONFIG to "0",
                ProducerConfig.RETRIES_CONFIG to "0",
                ProducerConfig.BATCH_SIZE_CONFIG to "0",
                SaslConfigs.SASL_MECHANISM to "PLAIN",
            ),
            StringSerializer(),
            StringSerializer(),
        )

    fun sendOgVentTilKonsumert(
        nøkkel: String,
        melding: String,
        topic: KafkaTopics,
    ) {
        runBlocking {
            val sendtMelding = kafkaProducer.send(ProducerRecord(topic.navnMedNamespace, nøkkel, melding)).get()
            ventTilKonsumert(
                konsumentGruppeId = topic.konsumentGruppe,
                recordMetadata = sendtMelding,
            )
        }
    }

    private suspend fun ventTilKonsumert(
        konsumentGruppeId: String,
        recordMetadata: RecordMetadata,
    ) = withTimeoutOrNull(Duration.ofSeconds(5)) {
        do {
            delay(timeMillis = 1L)
        } while (consumerSinOffset(
                consumerGroup = konsumentGruppeId,
                topic = recordMetadata.topic(),
            ) <= recordMetadata.offset()
        )
    }

    private fun consumerSinOffset(
        consumerGroup: String,
        topic: String,
    ): Long {
        val offsetMetadata = adminClient.listConsumerGroupOffsets(consumerGroup)
            .partitionsToOffsetAndMetadata().get()
        return offsetMetadata[offsetMetadata.keys.firstOrNull { it.topic().contains(topic) }]?.offset() ?: -1
    }

    fun sendVirksomhetsstatistikk(
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val virksomhetMelding = enStandardVirksomhetsMelding(år, kvartal)
                sendOgVentTilKonsumert(
                    nøkkel = virksomhetMelding.toJsonKey(),
                    melding = virksomhetMelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
                )
            }
        }
    }

    fun sendLandsstatistikk(
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val landmelding = enStandardLandMelding(år, kvartal)
                sendOgVentTilKonsumert(
                    nøkkel = landmelding.toJsonKey(),
                    melding = landmelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
                )
            }
        }
    }

    fun sendSektorstatistikk(
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val sektormelding = enStandardSektorMelding(år, kvartal)
                sendOgVentTilKonsumert(
                    nøkkel = sektormelding.toJsonKey(),
                    melding = sektormelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
                )
            }
        }
    }

    fun sendBransjestatistikk(
        bransje: String,
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val bransjemelding = enStandardBransjeMelding(år, kvartal, bransje)
                sendOgVentTilKonsumert(
                    nøkkel = bransjemelding.toJsonKey(),
                    melding = bransjemelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
                )
            }
        }
    }

    private fun enStandardVirksomhetsMelding(
        årstall: Int,
        kvartal: Int,
        orgnr: String = enUnderenhetIAltinn.orgnr,
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = orgnr,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            prosent = 28.3,
            tapteDagsverk = 154.5439,
            muligeDagsverk = 761.3,
            antallPersoner = 4,
            tapteDagsverGradert = 33.2,
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2,
                ),
            ),
        )

    private fun enStandardBransjeMelding(
        årstall: Int,
        kvartal: Int,
        bransje: String = "Sykehjem",
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.BRANSJE,
            kode = bransje,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            prosent = 5.8,
            tapteDagsverk = 270744.659570,
            muligeDagsverk = 4668011.371895,
            antallPersoner = 88563,
            tapteDagsverGradert = 1000.0,
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 93005.180000,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 4505.170000,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "C",
                    tapteDagsverk = 114144.140000,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 17410.030000,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 48317.900000,
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 5835.970000,
                ),
            ),
        )

    private fun enStandardSektorMelding(
        årstall: Int,
        kvartal: Int,
        sektor: String = "1",
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.SEKTOR,
            kode = sektor,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            tapteDagsverk = 1275292.330000,
            muligeDagsverk = 19790049.740000,
            prosent = 6.3,
            antallPersoner = 367239,
        )

    private fun enStandardLandMelding(
        årstall: Int,
        kvartal: Int,
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.LAND,
            kode = "NO",
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            tapteDagsverk = 11539578.440000,
            muligeDagsverk = 180204407.260000,
            prosent = 6.4,
            antallPersoner = 3365162,
        )
}
