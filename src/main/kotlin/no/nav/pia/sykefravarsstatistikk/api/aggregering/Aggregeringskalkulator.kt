package no.nav.pia.sykefravarsstatistikk.api.aggregering

import arrow.core.Either
import no.nav.pia.sykefravarsstatistikk.api.aggregering.SumAvSykefraværOverFlereKvartaler.Companion.NULLPUNKT
import no.nav.pia.sykefravarsstatistikk.api.dto.StatistikkJson
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.exceptions.Statistikkfeil

class Aggregeringskalkulator(
    private var sykefraværsdata: Sykefraværsdata,
    private var sistePubliserteKvartal: ÅrstallOgKvartal,
) {
    fun fraværsprosentNorge(): Either<Statistikkfeil, StatistikkJson> =
        summerOppSisteFireKvartaler(sykefraværsdata.sykefravær[Aggregeringskategorier.Land] ?: listOf())
            .regnUtProsentOgMapTilDto(Statistikkategori.LAND, "Norge")

    fun fraværsprosentBransjeEllerNæring(bransjeEllerNæring: BransjeEllerNæring): Either<Statistikkfeil, StatistikkJson> {
        val statistikk = sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Bransje }
            ?: sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Næring }
        return summerOppSisteFireKvartaler(statistikk?.value ?: emptyList())
            .regnUtProsentOgMapTilDto(
                bransjeEllerNæring.statistikkategori,
                bransjeEllerNæring.navn(),
            )
    }

    fun tapteDagsverkVirksomhet(bedriftsnavn: String): Either<Statistikkfeil, StatistikkJson> =
        summerOppSisteFireKvartaler(
            sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Virksomhet }?.value
                ?: emptyList(),
        ).getTapteDagsverkOgMapTilDto(Statistikkategori.VIRKSOMHET, bedriftsnavn)

    fun muligeDagsverkVirksomhet(bedriftsnavn: String): Either<Statistikkfeil, StatistikkJson> =
        summerOppSisteFireKvartaler(
            sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Virksomhet }?.value
                ?: emptyList(),
        ).getMuligeDagsverkOgMapTilDto(Statistikkategori.VIRKSOMHET, bedriftsnavn)

    fun fraværsprosentVirksomhet(virksomhetsnavn: String): Either<Statistikkfeil, StatistikkJson> =
        summerOppSisteFireKvartaler(
            sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Virksomhet }?.value
                ?: emptyList(),
        ).regnUtProsentOgMapTilDto(Statistikkategori.VIRKSOMHET, virksomhetsnavn)

    fun trendBransjeEllerNæring(bransjeEllerNæring: BransjeEllerNæring): Either<UtilstrekkeligData, StatistikkJson> {
        val statistikk = sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Bransje }?.value
            ?: sykefraværsdata.sykefravær.entries.find { it.key is Aggregeringskategorier.Næring }?.value
            ?: emptyList()
        val maybeTrend = Trendkalkulator(
            statistikk,
            sistePubliserteKvartal,
        ).kalkulerTrend()
        return maybeTrend.map { r: Trend ->
            r.tilAggregertHistorikkDto(
                bransjeEllerNæring.statistikkategori,
                bransjeEllerNæring.navn(),
            )
        }
    }

    fun summerOppSisteFireKvartaler(statistikk: List<UmaskertSykefraværUtenProsentForEttKvartal>): SumAvSykefraværOverFlereKvartaler =
        ekstraherSisteFireKvartaler(statistikk)
            .map { SumAvSykefraværOverFlereKvartaler(it) }
            .reduceOrNull { it, other -> it.leggSammen(other) } ?: NULLPUNKT

    private fun ekstraherSisteFireKvartaler(
        statistikk: List<UmaskertSykefraværUtenProsentForEttKvartal>,
    ): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        statistikk
            .filter {
                (sistePubliserteKvartal.sisteFireKvartaler()).contains(it.årstallOgKvartal)
            }.sorted()
}
