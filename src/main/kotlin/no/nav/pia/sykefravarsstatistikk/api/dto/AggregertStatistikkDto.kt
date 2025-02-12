package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkVirksomhet

@Serializable
data class AggregertStatistikkDto(
    val orgnr: String,
    val prosent: Double,
) {
    companion object {
        fun SykefraværsstatistikkVirksomhet.tilDto() = ""
    }
}
