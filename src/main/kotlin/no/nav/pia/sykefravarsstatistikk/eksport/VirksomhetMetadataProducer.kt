package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Kafka
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic

class VirksomhetMetadataProducer(
    kafka: Kafka,
    topic: Topic,
) : KafkaProdusent<VirksomhetMetadataKafkamelding>(kafka = kafka, topic = topic) {
    override fun tilKafkaMelding(input: VirksomhetMetadataKafkamelding): Pair<String, String> {
        val nøkkel = Json.encodeToString(
            value =
                VirksomhetMetadataNøkkel(orgnr = input.orgnr, årstall = input.årstall, kvartal = input.kvartal),
        )
        val verdi = Json.encodeToString(value = input)
        return nøkkel to verdi
    }
}

@Serializable
data class VirksomhetMetadataNøkkel(
    val orgnr: String,
    @SerialName("arstall")
    val årstall: Int,
    val kvartal: Int,
)

@Serializable
data class VirksomhetMetadataKafkamelding(
    val orgnr: String,
    @SerialName("arstall")
    val årstall: Int,
    val kvartal: Int,
    @SerialName("naring")
    val næring: String,
    @SerialName("naringskode")
    val næringskode: String,
    val bransje: String?,
    val sektor: String,
)
