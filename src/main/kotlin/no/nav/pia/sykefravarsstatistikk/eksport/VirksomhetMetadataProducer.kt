package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Kafka
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic

class VirksomhetMetadataProducer(
    kafka: Kafka,
    topic: Topic,
) : KafkaProdusent<VirksomhetMetadataKafkamelding>(kafka = kafka, topic = topic) {
    private val jsonMapper = ObjectMapper()

    override fun tilKafkaMelding(input: VirksomhetMetadataKafkamelding): Pair<String, String> {
        val nøkkel = jsonMapper.writeValueAsString(
            VirksomhetMetadataNøkkel(orgnr = input.orgnr, årstall = input.årstall, kvartal = input.kvartal),
        )
        val verdi = jsonMapper.writeValueAsString(input)
        return nøkkel to verdi
    }
}

@Serializable
data class VirksomhetMetadataNøkkel(
    val orgnr: String,
    @SerializedName("arstall")
    val årstall: Int,
    val kvartal: Int,
)

@Serializable
data class VirksomhetMetadataKafkamelding(
    val orgnr: String,
    @SerializedName("arstall")
    val årstall: Int,
    val kvartal: Int,
    @SerializedName("naring")
    val næring: String,
    @SerializedName("naringskode")
    val næringskode: String,
    val bransje: String?,
    val sektor: String,
)
