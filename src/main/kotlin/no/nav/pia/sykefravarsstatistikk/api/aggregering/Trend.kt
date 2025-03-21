package no.nav.pia.sykefravarsstatistikk.api.aggregering

import no.nav.pia.sykefravarsstatistikk.api.dto.StatistikkJson
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import java.math.BigDecimal

data class Trend(
    var trendverdi: BigDecimal? = null,
    var antallPersonerIBeregningen: Int = 0,
    var kvartalerIBeregningen: List<ÅrstallOgKvartal>? = null,
) {
    fun tilAggregertHistorikkDto(
        type: Statistikkategori,
        label: String,
    ): StatistikkJson =
        StatistikkJson(
            statistikkategori = type,
            label = label,
            verdi = trendverdi.toString(),
            antallPersonerIBeregningen = antallPersonerIBeregningen,
            kvartalerIBeregningen = kvartalerIBeregningen ?: emptyList(),
        )
}
