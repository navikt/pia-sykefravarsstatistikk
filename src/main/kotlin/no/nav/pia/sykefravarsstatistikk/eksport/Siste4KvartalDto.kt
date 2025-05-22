package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable

@Serializable
data class Siste4KvartalDto(
    val prosent: Double?,
    val tapteDagsverk: Double?,
    val muligeDagsverk: Double?,
    val erMaskert: Boolean,
    val kvartaler: List<KvartalDto>,
)

fun SykefraværFlereKvartalerForEksport.tilDto(): Siste4KvartalDto =
    Siste4KvartalDto(
        prosent = prosent?.toDouble(),
        tapteDagsverk = tapteDagsverk?.toDouble(),
        muligeDagsverk = muligeDagsverk?.toDouble(),
        erMaskert = erMaskert,
        kvartaler = kvartaler.map {
            KvartalDto(
                årstall = it.årstall,
                kvartal = it.kvartal,
            )
        },
    )
