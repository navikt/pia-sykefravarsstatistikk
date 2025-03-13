package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class AggregertStatistikkResponseDto(
    val prosentSiste4KvartalerTotalt: List<AggregertStatistikkDto>,
    val prosentSiste4KvartalerGradert: List<AggregertStatistikkDto>,
    val prosentSiste4KvartalerKorttid: List<AggregertStatistikkDto>,
    val prosentSiste4KvartalerLangtid: List<AggregertStatistikkDto>,
    val trendTotalt: List<AggregertStatistikkDto>,
    val tapteDagsverkTotalt: List<AggregertStatistikkDto>,
    val muligeDagsverkTotalt: List<AggregertStatistikkDto>,
)

@Serializable
data class AggregertStatistikkDto(
    val statistikkategori: String,
    val label: String,
    val verdi: String,
    val antallPersonerIBeregningen: Int,
    val kvartalerIBeregningen: List<KvartalIBeregning>,
)

@Serializable
data class KvartalIBeregning(
    val årstall: Int,
    val kvartal: Int,
)
