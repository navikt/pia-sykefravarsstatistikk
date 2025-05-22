package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.api.maskering.SykefraværMedKategori

@Serializable
data class SistePubliserteKvartalDto(
    val årstall: Int,
    val kvartal: Int,
    val prosent: Double?,
    val tapteDagsverk: Double?,
    val muligeDagsverk: Double?,
    val antallPersoner: Int?,
    val erMaskert: Boolean,
)

fun SykefraværMedKategori.tilDto(): SistePubliserteKvartalDto =
    SistePubliserteKvartalDto(
        årstall = årstall,
        kvartal = kvartal,
        prosent = prosent?.toDouble(),
        tapteDagsverk = tapteDagsverk?.toDouble(),
        muligeDagsverk = muligeDagsverk?.toDouble(),
        antallPersoner = antallPersoner,
        erMaskert = erMaskert,
    )
