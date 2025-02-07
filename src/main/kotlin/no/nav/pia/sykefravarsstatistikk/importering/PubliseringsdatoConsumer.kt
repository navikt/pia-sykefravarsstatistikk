package no.nav.pia.sykefravarsstatistikk.importering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.ApplikasjonsHelse
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaConfig
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaTopics
import no.nav.pia.sykefravarsstatistikk.persistering.MetadataService
import no.nav.pia.sykefravarsstatistikk.persistering.tilPubliseringsdatoDto
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.RetriableException
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.CoroutineContext

class PubliseringsdatoConsumer(
    val topic: KafkaTopics,
    val metadataService: MetadataService,
    val applikasjonsHelse: ApplikasjonsHelse,
) : CoroutineScope {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    private val job: Job = Job()
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
                                records.map {
                                    it.value().tilPubliseringsdatoDto()
                                }.let {
                                    metadataService.lagrePubliseringsdato(it)
                                }
                                logger.info("Lagret ${records.count()} meldinger i PubliseringsdatoConsumer (topic '$topic') ")
                                consumer.commitSync()
                                logger.info("Prosesserte ${records.count()} meldinger i topic: ${topic.navnMedNamespace}")
                            }
                        } catch (e: RetriableException) {
                            logger.warn(
                                "Had a retriable exception in PubliseringsdatoConsumer (topic '$topic'), retrying",
                                e,
                            )
                        }
                    }
                } catch (e: WakeupException) {
                    logger.info("PubliseringsdatoConsumer (topic '$topic')  is shutting down...")
                } catch (e: Exception) {
                    logger.error(
                        "Exception is shutting down kafka listner i PubliseringsdatoConsumer (topic '$topic')",
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
            logger.info("Stopping kafka consumer job i PubliseringsdatoConsumer (topic '$topic')")
            kafkaConsumer.wakeup()
            job.cancelAndJoin()
            logger.info("Stopped kafka consumer job i PubliseringsdatoConsumer (topic '$topic')")
        }
}
