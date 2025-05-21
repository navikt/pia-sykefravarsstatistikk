package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Kafka
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic

class SykefraværsstatistikkProducer(
    kafka: Kafka,
    topic: Topic,
) : KafkaProdusent<StatistikkategoriKafkamelding>(kafka = kafka, topic = topic) {
    private val jsonMapper = ObjectMapper()

    override fun tilKafkaMelding(input: StatistikkategoriKafkamelding): Pair<String, String> {
        val nøkkel: String = jsonMapper.writeValueAsString(
            mapOf(
                "kategori" to input.sisteKvartal.kategori,
                "kode" to input.sisteKvartal.kode,
                "kvartal" to input.sisteKvartal.kvartal.toString(),
                "årstall" to input.sisteKvartal.årstall.toString(),
            ),
        )

        val verdi = SykefraværsstatistikkPerKategoriEksportDto(
            kategori = input.sisteKvartal.kategori,
            kode = input.sisteKvartal.kode,
            sistePubliserteKvartal = SistePubliserteKvartal(
                årstall = input.sisteKvartal.årstall,
                kvartal = input.sisteKvartal.kvartal,
                prosent = input.sisteKvartal.prosent?.toDouble(),
                tapteDagsverk = input.sisteKvartal.tapteDagsverk?.toDouble(),
                muligeDagsverk = input.sisteKvartal.muligeDagsverk?.toDouble(),
                antallPersoner = input.sisteKvartal.antallPersoner,
                erMaskert = input.sisteKvartal.erMaskert,
            ),
            siste4Kvartal = Siste4Kvartal(
                prosent = input.siste4Kvartal.prosent?.toDouble(),
                tapteDagsverk = input.siste4Kvartal.tapteDagsverk?.toDouble(),
                muligeDagsverk = input.siste4Kvartal.muligeDagsverk?.toDouble(),
                erMaskert = input.siste4Kvartal.erMaskert,
                kvartaler = input.siste4Kvartal.kvartaler.map {
                    Kvartal(
                        årstall = it.årstall,
                        kvartal = it.kvartal,
                    )
                },
            ),
        )
        return nøkkel to Json.encodeToString(verdi)
    }

    @Serializable
    data class SykefraværsstatistikkPerKategoriEksportDto(
        val kategori: Statistikkategori,
        val kode: String,
        val sistePubliserteKvartal: SistePubliserteKvartal,
        val siste4Kvartal: Siste4Kvartal,
    )

    @Serializable
    data class Siste4Kvartal(
        val prosent: Double?,
        val tapteDagsverk: Double?,
        val muligeDagsverk: Double?,
        val erMaskert: Boolean,
        val kvartaler: List<Kvartal>,
    )

    @Serializable
    data class Kvartal(
        val årstall: Int,
        val kvartal: Int,
    )

    @Serializable
    data class SistePubliserteKvartal(
        val årstall: Int,
        val kvartal: Int,
        val prosent: Double?,
        val tapteDagsverk: Double?,
        val muligeDagsverk: Double?,
        val antallPersoner: Int?,
        val erMaskert: Boolean,
    )
}
