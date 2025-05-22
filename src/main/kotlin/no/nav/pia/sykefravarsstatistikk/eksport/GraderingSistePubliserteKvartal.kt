package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.api.maskering.SykefraværMedKategori

@Serializable
data class GraderingSistePubliserteKvartal(
    val årstall: Int,
    val kvartal: Int,
    val prosent: Double?,
    val tapteDagsverkGradert: Double?,
    val tapteDagsverk: Double?,
    val antallPersoner: Int?,
    val erMaskert: Boolean,
)

fun SykefraværMedKategori.tilGradertDto(): GraderingSistePubliserteKvartal =
    GraderingSistePubliserteKvartal(
        årstall = årstall,
        kvartal = kvartal,
        prosent = prosent?.toDouble(),
        tapteDagsverkGradert = tapteDagsverk?.toDouble(),
        tapteDagsverk = muligeDagsverk?.toDouble(),
        antallPersoner = antallPersoner,
        erMaskert = erMaskert,
    )
