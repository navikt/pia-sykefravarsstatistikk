package no.nav.pia.sykefravarsstatistikk.domene

import kotlinx.serialization.Serializable

@Serializable
data class BrregNæringskodeDto(
    val kode: String,
    val beskrivelse: String,
)
