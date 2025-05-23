package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable

@Serializable
data class GraderingSiste4Kvartal(
    val prosent: Double?,
    val tapteDagsverkGradert: Double?,
    val tapteDagsverk: Double?,
    val erMaskert: Boolean,
    val kvartaler: List<KvartalDto>,
)

fun SykefraværFlereKvartalerForEksport.tilGradertDto(): GraderingSiste4Kvartal =
    GraderingSiste4Kvartal(
        prosent = prosent?.toDouble(),
        tapteDagsverkGradert = tapteDagsverk?.toDouble(),
        // TODO: kopiert fra sykefravarsstatistikk, men er det rett at 'tapteDagsverkGradert = tapteDagsverk'? Burde det refactores til noe mer leselig?
        tapteDagsverk = muligeDagsverk?.toDouble(),
        // TODO: kopiert fra sykefravarsstatistikk, men er det rett at 'tapteDagsverk = muligeDagsverk'? Burde det refactores til noe mer leselig?
        erMaskert = erMaskert,
        kvartaler = kvartaler.map {
            KvartalDto(
                årstall = it.årstall,
                kvartal = it.kvartal,
            )
        },
    )
