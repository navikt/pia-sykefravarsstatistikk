package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable

@Serializable
data class KvartalDto(
    val årstall: Int,
    val kvartal: Int,
)
