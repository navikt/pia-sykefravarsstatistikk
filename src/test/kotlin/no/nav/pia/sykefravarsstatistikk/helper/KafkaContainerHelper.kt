package no.nav.pia.sykefravarsstatistikk.helper

import ia.felles.definisjoner.bransjer.Bransje
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.time.withTimeoutOrNull
import no.nav.pia.sykefravarsstatistikk.domene.Konstanter.MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.PubliseringsdatoImportTestUtils.Companion.toJson
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.JsonMelding
import no.nav.pia.sykefravarsstatistikk.helper.SykefraværsstatistikkImportTestUtils.TapteDagsverkPerVarighet
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaConfig
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import no.nav.pia.sykefravarsstatistikk.persistering.PubliseringsdatoDto
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

    fun sendPubliseringsdatoer(publiseringsdatoer: List<PubliseringsdatoDto>) {
        publiseringsdatoer.forEach {
            val key = PubliseringsdatoImportTestUtils.PubliseringsdatoJsonKey(
                rapportPeriode = it.rapportPeriode,
            )
            val value = PubliseringsdatoImportTestUtils.PubliseringsdatoJsonValue(
                rapportPeriode = it.rapportPeriode,
                offentligDato = it.offentligDato,
                oppdatertIDvh = it.oppdatertIDvh,
            )
            sendOgVentTilKonsumert(
                nøkkel = key.toJson(),
                melding = value.toJson(),
                topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_PUBLISERINGSDATO,
            )
        }
    }

    fun sendStatistikk(
        underenhet: Underenhet.Næringsdrivende,
        overordnetEnhet: OverordnetEnhet,
    ) {
        sendLandsstatistikk()

        sendSektorstatistikk(overordnetEnhet.sektor!!)

        underenhet.bransje()?.let { bransje -> sendBransjestatistikk(bransje = bransje) }
            ?: sendNæringsstatistikk(næring = underenhet.næringskode.næring)

        sendVirksomhetsstatistikk(virksomhet = underenhet)
        sendVirksomhetsstatistikk(virksomhet = overordnetEnhet)
    }

    fun sendEnkelVirksomhetsstatistikk(
        virksomhet: Virksomhet,
        årstall: Int = 2010,
        harForFåAnsatte: Boolean = false,
    ) {
        for (kvartal in 1..4) {
            val virksomhetMelding = if (harForFåAnsatte) {
                enVirksomhetsMeldingMedFåAnsatte(årstall = årstall, kvartal = kvartal, virksomhet = virksomhet)
            } else {
                enStandardVirksomhetsMelding(årstall = årstall, kvartal = kvartal, virksomhet = virksomhet)
            }
            sendOgVentTilKonsumert(
                nøkkel = virksomhetMelding.toJsonKey(),
                melding = virksomhetMelding.toJsonValue(),
                topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET,
            )
        }
    }

    fun sendVirksomhetsstatistikk(
        virksomhet: Virksomhet,
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val virksomhetMelding =
                    enStandardVirksomhetsMelding(årstall = år, kvartal = kvartal, virksomhet = virksomhet)
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
        sektor: Sektor,
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val sektormelding = enStandardSektorMelding(
                    årstall = år,
                    kvartal = kvartal,
                    sektor = sektor,
                )
                sendOgVentTilKonsumert(
                    nøkkel = sektormelding.toJsonKey(),
                    melding = sektormelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
                )
            }
        }
    }

    fun sendBransjestatistikk(
        bransje: Bransje,
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val bransjemelding = enStandardBransjeMelding(årstall = år, kvartal = kvartal, bransje = bransje)
                sendOgVentTilKonsumert(
                    nøkkel = bransjemelding.toJsonKey(),
                    melding = bransjemelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
                )
            }
        }
    }

    fun sendNæringsstatistikk(
        næring: Næring,
        startÅr: Int = 2010,
        sluttÅr: Int = 2024,
        harForFåAnsatte: Boolean = false,
    ) {
        for (år in startÅr..sluttÅr) {
            for (kvartal in 1..4) {
                val næringMelding = if (harForFåAnsatte) {
                    enMeldingMedFåAnsatte(
                        årstall = år,
                        kvartal = kvartal,
                        statistikkategori = Statistikkategori.NÆRING,
                        kode = næring.tosifferIdentifikator,
                    )
                } else {
                    enStandardNæringMelding(årstall = år, kvartal = kvartal, næring = næring)
                }
                sendOgVentTilKonsumert(
                    nøkkel = næringMelding.toJsonKey(),
                    melding = næringMelding.toJsonValue(),
                    topic = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER,
                )
            }
        }
    }

    private fun enMeldingMedFåAnsatte(
        årstall: Int,
        kvartal: Int,
        statistikkategori: Statistikkategori,
        kode: String,
    ): JsonMelding =
        JsonMelding(
            kategori = statistikkategori,
            kode = kode,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            prosent = 0.2.toBigDecimal(),
            tapteDagsverk = 2.0.toBigDecimal(),
            muligeDagsverk = 1000.0.toBigDecimal(),
            antallPersoner = (MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER - 1),
            tapteDagsverGradert = 0.5.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 2.0.toBigDecimal(),
                ),
            ),
        )

    private fun enVirksomhetsMeldingMedFåAnsatte(
        årstall: Int,
        kvartal: Int,
        virksomhet: Virksomhet,
    ): JsonMelding =
        enMeldingMedFåAnsatte(
            årstall = årstall,
            kvartal = kvartal,
            statistikkategori = Statistikkategori.VIRKSOMHET,
            kode = virksomhet.orgnr,
        )

    private fun enStandardVirksomhetsMelding(
        årstall: Int,
        kvartal: Int,
        virksomhet: Virksomhet,
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.VIRKSOMHET,
            kode = virksomhet.orgnr,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            prosent = 28.3.toBigDecimal(),
            tapteDagsverk = 154.5439.toBigDecimal(),
            muligeDagsverk = 761.3.toBigDecimal(),
            antallPersoner = 14,
            tapteDagsverGradert = 33.2.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 12.3.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 5.2.toBigDecimal(),
                ),
            ),
        )

    private fun enStandardBransjeMelding(
        årstall: Int,
        kvartal: Int,
        bransje: Bransje,
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.BRANSJE,
            kode = bransje.navn,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            prosent = 5.8.toBigDecimal(),
            tapteDagsverk = 270744.659570.toBigDecimal(),
            muligeDagsverk = 4668011.371895.toBigDecimal(),
            antallPersoner = 88563,
            tapteDagsverGradert = 1000.0.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 93005.180000.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 4505.170000.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "C",
                    tapteDagsverk = 114144.140000.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 17410.030000.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 48317.900000.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 5835.970000.toBigDecimal(),
                ),
            ),
        )

    private fun enStandardNæringMelding(
        årstall: Int,
        kvartal: Int,
        næring: Næring,
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.NÆRING,
            kode = næring.tosifferIdentifikator,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            prosent = 5.9.toBigDecimal(),
            tapteDagsverk = 73154.250363.toBigDecimal(),
            muligeDagsverk = 1239902.548524.toBigDecimal(),
            antallPersoner = 25122,
            tapteDagsverGradert = 28655.128516.toBigDecimal(),
            tapteDagsverkMedVarighet = listOf(
                TapteDagsverkPerVarighet(
                    varighet = "A",
                    tapteDagsverk = 7925.03.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "B",
                    tapteDagsverk = 30269.75.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "C",
                    tapteDagsverk = 474.21.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "D",
                    tapteDagsverk = 11120.86.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "E",
                    tapteDagsverk = 9487.93.toBigDecimal(),
                ),
                TapteDagsverkPerVarighet(
                    varighet = "F",
                    tapteDagsverk = 13876.47.toBigDecimal(),
                ),
            ),
        )

    private fun enStandardSektorMelding(
        årstall: Int,
        kvartal: Int,
        sektor: Sektor,
    ): JsonMelding =
        JsonMelding(
            kategori = Statistikkategori.SEKTOR,
            kode = sektor.kode,
            årstallOgKvartal = ÅrstallOgKvartal(årstall = årstall, kvartal = kvartal),
            tapteDagsverk = 1275292.330000.toBigDecimal(),
            muligeDagsverk = 19790049.740000.toBigDecimal(),
            prosent = 6.3.toBigDecimal(),
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
            tapteDagsverk = 11539578.440000.toBigDecimal(),
            muligeDagsverk = 180204407.260000.toBigDecimal(),
            prosent = 6.4.toBigDecimal(),
            antallPersoner = 3365162,
        )
}
