package no.nav.pia.sykefravarsstatistikk.api.maskering

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import no.nav.pia.sykefravarsstatistikk.api.maskering.MaskertKvartalsvisSykefraværshistorikkDto.Companion.tilMaskertBigDecimal
import no.nav.pia.sykefravarsstatistikk.domene.Konstanter.MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import java.math.BigDecimal

@Serializable
data class MaskertKvartalsvisSykefraværshistorikkDto(
    val type: String,
    val label: String,
    val kvartalsvisSykefraværsprosent: List<MaskertKvartalsvisSykefraværsprosentDto>,
) {
    companion object {
        fun List<Sykefraværsstatistikk>.tilMaskertDto(
            type: String,
            label: String,
        ) = MaskertKvartalsvisSykefraværshistorikkDto(
            type = type,
            label = label,
            kvartalsvisSykefraværsprosent = this.map { sykefraværsstatistikk ->
                val erMaskert =
                    sykefraværsstatistikk.antallPersoner < MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER
                MaskertKvartalsvisSykefraværsprosentDto(
                    tapteDagsverk = sykefraværsstatistikk.tapteDagsverk.masker(sykefraværsstatistikk.antallPersoner),
                    muligeDagsverk = sykefraværsstatistikk.muligeDagsverk.masker(sykefraværsstatistikk.antallPersoner),
                    prosent = sykefraværsstatistikk.prosent.masker(sykefraværsstatistikk.antallPersoner),
                    erMaskert = erMaskert,
                    årstall = sykefraværsstatistikk.årstall,
                    kvartal = sykefraværsstatistikk.kvartal,
                )
            },
        )

        fun BigDecimal?.tilMaskertBigDecimal(): MaskertBigDecimal =
            if (this == null) {
                MaskertBigDecimal(
                    umaskertVerdi = null,
                    erMaskert = true,
                )
            } else {
                MaskertBigDecimal(
                    umaskertVerdi = this,
                    erMaskert = false,
                )
            }

        fun BigDecimal.masker(antallPersoner: Int): MaskertBigDecimal =
            MaskertBigDecimal(
                umaskertVerdi = if (antallPersoner < MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER) {
                    null
                } else {
                    this
                },
                erMaskert = antallPersoner < MIN_ANTALL_PERS_FOR_AT_STATISTIKKEN_IKKE_ER_PERSONOPPLYSNINGER,
            )
    }
}

@Serializable
data class MaskertKvartalsvisSykefraværsprosentDto(
    val årstall: Int,
    val kvartal: Int,
    val tapteDagsverk: MaskertBigDecimal,
    val muligeDagsverk: MaskertBigDecimal,
    val prosent: MaskertBigDecimal,
    val erMaskert: Boolean,
)

@Serializable(with = MaksertBigDecimalSerializer::class)
class MaskertBigDecimal(
    val umaskertVerdi: BigDecimal?,
    val erMaskert: Boolean,
) {
    val verdi get() = this.umaskertVerdi
}

@OptIn(ExperimentalSerializationApi::class)
internal object MaksertBigDecimalSerializer : KSerializer<MaskertBigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.DOUBLE)

    /**
     * Parse til en [BigDecimal] fra raw content med [JsonDecoder.decodeJsonElement],
     *  eller med [Decoder.decodeString] hvis verdien kommer som en [String]
     */
    override fun deserialize(decoder: Decoder): MaskertBigDecimal =
        when (decoder) {
            is JsonDecoder -> decoder.decodeJsonElement().jsonPrimitive.content.toBigDecimal().tilMaskertBigDecimal()
            else -> decoder.decodeString().toBigDecimal().tilMaskertBigDecimal()
        }

    /**
     * Bruk av [JsonUnquotedLiteral] for å produsere en [BigDecimal] verdi uten ""
     *  eller, produserer en [value] med "" ved å bruke [Encoder.encodeString].
     */
    override fun serialize(
        encoder: Encoder,
        value: MaskertBigDecimal,
    ) = when (encoder) {
        is JsonEncoder -> encoder.encodeJsonElement(JsonUnquotedLiteral(if (value.verdi == null) null else value.verdi!!.toPlainString()))
        else -> encoder.encodeString(if (value.verdi == null) "" else value.verdi!!.toPlainString())
    }
}
