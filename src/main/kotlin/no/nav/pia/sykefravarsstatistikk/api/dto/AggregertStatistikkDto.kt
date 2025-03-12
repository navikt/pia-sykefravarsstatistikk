package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk

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
) {
    companion object {
        // TODO: På sikt, flytt til ia-felles og ha det i en private/sealed klasse
        private fun List<Sykefraværsstatistikk>.muligeDagsverkTotalt() = sumOf { it.muligeDagsverk }

        private fun List<Sykefraværsstatistikk>.tapteDagsverkTotalt() = sumOf { it.tapteDagsverk }

        private fun List<Sykefraværsstatistikk>.prosentLangTid() = map { it.prosent }.average()

        private fun List<Sykefraværsstatistikk>.prosentKortTid() = map { it.prosent }.average()

        private fun List<Sykefraværsstatistikk>.prosentGradert() = tapteDagsverkTotalt() / muligeDagsverkTotalt() * 100

        private fun List<Sykefraværsstatistikk>.personerIBeregning() = map { it.antallPersoner }.average().toInt()

        private fun List<Sykefraværsstatistikk>.trendTotalt() = -1.0

        private fun List<Sykefraværsstatistikk>.prosentTotalt() = tapteDagsverkTotalt() / muligeDagsverkTotalt() * 100

        private fun List<Sykefraværsstatistikk>.kvartalerIBeregning() = map { KvartalIBeregning(it.årstall, it.kvartal) }

        fun List<Sykefraværsstatistikk>.prosentTotaltAggregert(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = "%.1f".format(prosentTotalt()),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )

        fun List<Sykefraværsstatistikk>.prosentGradertAggregert(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = "%.1f".format(prosentGradert()),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )

        fun List<Sykefraværsstatistikk>.prosentKortTidAggregert(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = "%.1f".format(prosentKortTid()),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )

        fun List<Sykefraværsstatistikk>.prosentLangTidAggregert(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = "%.1f".format(prosentLangTid()),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )

        fun List<Sykefraværsstatistikk>.trendTotaltAggregert(
            statistikkategori: Statistikkategori = BRANSJE,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = trendTotalt().toString(),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )

        fun List<Sykefraværsstatistikk>.tapteDagsverkTotaltAggregert(
            statistikkategori: Statistikkategori = VIRKSOMHET,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = tapteDagsverkTotalt().toString(),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )

        fun List<Sykefraværsstatistikk>.muligeDagsverkTotaltAggregert(
            statistikkategori: Statistikkategori = VIRKSOMHET,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = muligeDagsverkTotalt().toString(),
            antallPersonerIBeregningen = personerIBeregning(),
            kvartalerIBeregningen = kvartalerIBeregning(),
        )
    }

    @Serializable
    data class KvartalIBeregning(
        val årstall: Int,
        val kvartal: Int,
    )
}
