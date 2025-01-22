package no.nav.pia.sykefravarsstatistikk.importering

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.serializeToSykefraværsstatistikkDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KafkaImportMelding {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun ConsumerRecords<String, String>.toSykefraværsstatistikkImportKafkaMelding(): List<SykefraværsstatistikkImportKafkaMelding> =
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

        fun List<SykefraværsstatistikkImportKafkaMelding>.toSykefraværsstatistikkDto(): List<SykefraværsstatistikkDto> =
            this.map {
                when (it.nøkkel.kategori) {
                    LAND -> it.verdi.toSykefraværsstatistikkDto()
                    SEKTOR -> it.verdi.toSykefraværsstatistikkDto()
                    VIRKSOMHET -> it.verdi.toSykefraværsstatistikkDto()
                    else -> throw RuntimeException("Ukjent kategori")
                }
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
