package no.nav.pia.sykefravarsstatistikk.importering

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.serializeToSykefraværsstatistikkDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaImportMelding {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun ConsumerRecord<String, String>.toSykefraværsstatistikkImportKafkaMelding(): SykefraværsstatistikkImportKafkaMelding {
            val key = Json.decodeFromString<SykefraværsstatistikkImportKafkaMeldingNøkkel>(this.key())
            return SykefraværsstatistikkImportKafkaMelding(nøkkel = key, verdi = this.value())
        }

        fun erNøkkelGyldig(consumerRecord: ConsumerRecord<String, String>): Boolean {
            val key = Json.decodeFromString<SykefraværsstatistikkImportKafkaMeldingNøkkel>(consumerRecord.key())
            val nøkkelErGyldig = Statistikkategori.entries.contains(key.kategori) && key.kode.isNotEmpty()
            if (!nøkkelErGyldig) {
                logger.warn(
                    "Feil formatert Kafka melding i topic ${consumerRecord.topic()} for key ${
                        consumerRecord.key().trim()
                    }",
                )
            }
            return nøkkelErGyldig
        }

        fun String.toSykefraværsstatistikkDto(): SykefraværsstatistikkDto = this.serializeToSykefraværsstatistikkDto()

        fun List<SykefraværsstatistikkImportKafkaMelding>.toSykefraværsstatistikkDto(): List<SykefraværsstatistikkDto> =
            this.map {
                it.verdi.toSykefraværsstatistikkDto()
            }
    }
}

data class SykefraværsstatistikkImportKafkaMelding(
    val nøkkel: SykefraværsstatistikkImportKafkaMeldingNøkkel,
    val verdi: String,
)

@Serializable
data class SykefraværsstatistikkImportKafkaMeldingNøkkel(
    val årstall: Int,
    val kvartal: Int,
    val kategori: Statistikkategori,
    val kode: String,
)
