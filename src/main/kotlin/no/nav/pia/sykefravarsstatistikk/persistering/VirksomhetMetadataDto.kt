package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class VirksomhetMetadataDto(
    val orgnr: String,
    val årstall: Int,
    val kvartal: Int,
    val sektor: String,
    val primærnæring: String?,
    val primarnæringskode: String?,
    val rectype: String,
)

fun String.tilVirksomhetMetadataDto(): VirksomhetMetadataDto = Json.decodeFromString<VirksomhetMetadataDto>(this)
