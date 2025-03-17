package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.persistering.BigDecimalSerializer
import java.math.BigDecimal

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
                    erMaskert = sykefraværsstatistikk.antallPersoner < 5, // TODO: Implement maskering (+ test)
                    årstall = sykefraværsstatistikk.årstall,
                    kvartal = sykefraværsstatistikk.kvartal,
                )
            },
        )
    }
}

@Serializable
data class KvartalsvisSykefraværsprosentDto(
    @Serializable(with = BigDecimalSerializer::class)
    val tapteDagsverk: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val muligeDagsverk: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val prosent: BigDecimal,
    val erMaskert: Boolean,
    val årstall: Int,
    val kvartal: Int,
)
