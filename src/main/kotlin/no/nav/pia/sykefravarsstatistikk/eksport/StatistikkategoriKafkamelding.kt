package no.nav.pia.sykefravarsstatistikk.eksport

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.maskering.SykefraværMedKategori

data class StatistikkategoriKafkamelding(
    val sisteKvartal: SykefraværMedKategori,
    val siste4Kvartal: SykefraværFlereKvartalerForEksport,
) : Kafkamelding {
    private val jsonMapper = ObjectMapper()

    override val nøkkel: String
        get() = jsonMapper.writeValueAsString(
            mapOf(
                "kategori" to sisteKvartal.kategori,
                "kode" to sisteKvartal.kode,
                "kvartal" to sisteKvartal.kvartal.toString(),
                "årstall" to sisteKvartal.årstall.toString(),
            ),
        )

    override val innhold: String
        get() = Json.encodeToString(this.tilDto())
}
