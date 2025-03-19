package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Konstanter.MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
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
                val erMaskert = sykefraværsstatistikk.antallPersoner < MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
                KvartalsvisSykefraværsprosentDto(
                    tapteDagsverk = sykefraværsstatistikk.tapteDagsverk.masker(erMaskert),
                    muligeDagsverk = sykefraværsstatistikk.muligeDagsverk.masker(erMaskert),
                    prosent = sykefraværsstatistikk.prosent.masker(erMaskert),
                    erMaskert = erMaskert,
                    årstall = sykefraværsstatistikk.årstall,
                    kvartal = sykefraværsstatistikk.kvartal,
                )
            },
        )

        fun BigDecimal.masker(skalVæreMaskert: Boolean): BigDecimal? =
            if (skalVæreMaskert) {
                null
            } else {
                this
            }
    }
}

@Serializable
data class KvartalsvisSykefraværsprosentDto(
    @Serializable(with = BigDecimalSerializer::class)
    val tapteDagsverk: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val muligeDagsverk: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val prosent: BigDecimal?,
    val erMaskert: Boolean,
    val årstall: Int,
    val kvartal: Int,
)
