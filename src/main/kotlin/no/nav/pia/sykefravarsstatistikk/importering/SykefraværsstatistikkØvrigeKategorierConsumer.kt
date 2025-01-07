package no.nav.pia.sykefravarsstatistikk.importering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.ApplikasjonsHelse
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaConfig
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkService
import no.nav.pia.sykefravarsstatistikk.persistering.serializeToSykefraværsstatistikkDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class SykefraværsstatistikkØvrigeKategorierConsumer(
    val sykefraværsstatistikkService: SykefraværsstatistikkService,
    val applikasjonsHelse: ApplikasjonsHelse,
) : CoroutineScope {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val job: Job = Job()
    private val topic: KafkaTopics = KafkaTopics.KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER
    private val kafkaConsumer = KafkaConsumer(
        KafkaConfig().consumerProperties(konsumentGruppe = topic.konsumentGruppe),
        StringDeserializer(),
        StringDeserializer(),
    )

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    init {
        Runtime.getRuntime().addShutdownHook(Thread(this::cancel))
    }

    fun run() {
        launch {
            kafkaConsumer.use { consumer ->
                try {
                    consumer.subscribe(listOf(topic.navnMedNamespace))
                    logger.info(
                        "Kafka consumer subscribed to topic '${topic.navnMedNamespace}' of groupId '${topic.konsumentGruppe}' )' in SykefraværsstatistikkØvrigeKategorierConsumer",
                    )
                    while (applikasjonsHelse.alive) {
                        try {
                            val records = consumer.poll(Duration.ofSeconds(1))
                            if (!records.isEmpty) {
                                records.toSykefraværsstatistikkImportKafkaMelding().let {
                                    sykefraværsstatistikkService.lagreSykefraværsstatistikk(
                                        it.toSykefraværsstatistikkDto(),
                                    )
                                }
                                logger.info(
                                    "Lagret ${records.count()} meldinger i SykefraværsstatistikkØvrigeKategorierConsumer (topic '$topic') ",
                                )
                                consumer.commitSync()
                                logger.info("Prosesserte ${records.count()} meldinger i topic: ${topic.navnMedNamespace}")
                            }
                        } catch (e: RetriableException) {
                            logger.warn(
                                "Had a retriable exception in SykefraværsstatistikkØvrigeKategorierConsumer (topic '$topic'), retrying",
                                e,
                            )
                        }
                    }
                } catch (e: WakeupException) {
                    logger.info("StatistikkPerKategoriConsumer (topic '$topic')  is shutting down...")
                } catch (e: Exception) {
                    logger.error(
                        "Exception is shutting down kafka listner i SykefraværsstatistikkØvrigeKategorierConsumer (topic '$topic')",
                        e,
                    )
                    applikasjonsHelse.ready = false
                    applikasjonsHelse.alive = false
                }
            }
        }
    }

    private fun cancel() =
        runBlocking {
            logger.info("Stopping kafka consumer job i SykefraværsstatistikkØvrigeKategorierConsumer (topic '$topic')")
            kafkaConsumer.wakeup()
            job.cancelAndJoin()
            logger.info("Stopped kafka consumer job i SykefraværsstatistikkØvrigeKategorierConsumer (topic '$topic')")
        }

    private fun ConsumerRecords<String, String>.toSykefraværsstatistikkImportKafkaMelding(): List<SykefraværsstatistikkImportKafkaMelding> =
        this.filter {
            erMeldingenGyldig(it)
        }.map {
            val key = Json.decodeFromString<SykefraværsstatistikkImportKafkaMeldingNøkkel>(it.key())
            SykefraværsstatistikkImportKafkaMelding(nøkkel = key, verdi = it.value())
        }

    private fun erMeldingenGyldig(consumerRecord: ConsumerRecord<String, String>): Boolean {
        val key = Json.decodeFromString<SykefraværsstatistikkImportKafkaMeldingNøkkel>(consumerRecord.key())

        return if (Statistikkategori.entries.map { it }.contains(key.kategori) && key.kode.isNotEmpty()) {
            true
        } else {
            logger.warn(
                "Feil formatert Kafka melding i topic ${consumerRecord.topic()} for key ${
                    consumerRecord.key().trim()
                }",
            )
            false
        }
    }

    fun String.toSykefraværsstatistikkDto(): SykefraværsstatistikkDto = this.serializeToSykefraværsstatistikkDto()

    private fun List<SykefraværsstatistikkImportKafkaMelding>.toSykefraværsstatistikkDto(): List<SykefraværsstatistikkDto> =
        this.map {
            when (it.nøkkel.kategori) {
                LAND -> it.verdi.toSykefraværsstatistikkDto()
                VIRKSOMHET -> it.verdi.toSykefraværsstatistikkDto()
                else -> throw RuntimeException("Ukjent kategori")
            }
        }
}
