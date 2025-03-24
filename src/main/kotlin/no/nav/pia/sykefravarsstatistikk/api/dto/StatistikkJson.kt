package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal

@Serializable
data class AggregertStatistikkResponseDto(
    val prosentSiste4KvartalerTotalt: List<StatistikkJson>,
    val prosentSiste4KvartalerGradert: List<StatistikkJson>,
    val prosentSiste4KvartalerKorttid: List<StatistikkJson>,
    val prosentSiste4KvartalerLangtid: List<StatistikkJson>,
    val trendTotalt: List<StatistikkJson>,
    val tapteDagsverkTotalt: List<StatistikkJson>,
    val muligeDagsverkTotalt: List<StatistikkJson>,
)

@Serializable
data class StatistikkJson(
    val statistikkategori: Statistikkategori,
    val label: String,
    val verdi: String,
    val antallPersonerIBeregningen: Int,
    val kvartalerIBeregningen: List<ÅrstallOgKvartal>,
)
