package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk

@Serializable
data class KvartalsvisSykefraværshistorikkDto(
    val type: String,
    val label: String,
    val kvartalsvisSykefraværsprosent: List<KvartalsvisSykefraværsprosentDto>,
) {
    companion object {
        fun List<Sykefraværsstatistikk>.tilDto(
            type: String,
            label: String,
        ) = KvartalsvisSykefraværshistorikkDto(
            type = type,
            label = label,
            kvartalsvisSykefraværsprosent = this.map { sykefraværsstatistikk ->
                KvartalsvisSykefraværsprosentDto(
                    tapteDagsverk = sykefraværsstatistikk.tapteDagsverk,
                    muligeDagsverk = sykefraværsstatistikk.muligeDagsverk,
                    prosent = sykefraværsstatistikk.prosent,
                    erMaskert = false,
                    årstall = sykefraværsstatistikk.årstall,
                    kvartal = sykefraværsstatistikk.kvartal,
                )
            },
        )
    }
}

@Serializable
data class KvartalsvisSykefraværsprosentDto(
    val tapteDagsverk: Double,
    val muligeDagsverk: Double,
    val prosent: Double,
    val erMaskert: Boolean,
    val årstall: Int,
    val kvartal: Int,
)
