package no.nav.pia.sykefravarsstatistikk.api.dto

import ia.felles.definisjoner.bransjer.Bransje
import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.muligeDagsverkTotalt
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentGradert
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentKortTid
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.prosentLangTid
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.tapteDagsverkTotalt
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.Companion.tilTrendTotalt
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkDto.KvartalIBeregning.Companion.tilKvartalIBeregning
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.BRANSJE
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkBransje
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkLand
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkVirksomhet

@Serializable
data class SamletAggregertStatistikkDto(
    val prosentSiste4KvartalerTotalt: List<AggregertStatistikkDto>,
    val prosentSiste4KvartalerGradert: List<AggregertStatistikkDto>,
    val prosentSiste4KvartalerKorttid: List<AggregertStatistikkDto>,
    val prosentSiste4KvartalerLangtid: List<AggregertStatistikkDto>,
    val trendTotalt: List<AggregertStatistikkDto>,
    val tapteDagsverkTotalt: List<AggregertStatistikkDto>,
    val muligeDagsverkTotalt: List<AggregertStatistikkDto>,
) {
    companion object {
        fun lagAggregertStatistikk(
            statistikkLand: List<SykefraværsstatistikkLand>,
            statistikkBransje: List<SykefraværsstatistikkBransje>,
            statistikkVirksomhet: List<SykefraværsstatistikkVirksomhet>,
            bransje: Bransje,
            virksomhetsNavn: String = "SPISS SJOKKERT TIGER AS", // TODO: Hente fra enhetsregisteret ?
        ) = SamletAggregertStatistikkDto(
            prosentSiste4KvartalerTotalt = listOf(), // Virksomhet, Bransje, Land
            prosentSiste4KvartalerGradert = listOf(
                statistikkVirksomhet.prosentGradert(statistikkategori = VIRKSOMHET, label = virksomhetsNavn),
                statistikkBransje.prosentGradert(statistikkategori = BRANSJE, label = bransje.navn),
            ), // Virksomhet, Bransje
            prosentSiste4KvartalerKorttid = listOf(
                statistikkVirksomhet.prosentKortTid(statistikkategori = VIRKSOMHET, label = virksomhetsNavn),
                statistikkBransje.prosentKortTid(statistikkategori = BRANSJE, label = bransje.navn),
            ), // Virksomhet, Bransje
            prosentSiste4KvartalerLangtid = listOf(
                statistikkVirksomhet.prosentLangTid(statistikkategori = VIRKSOMHET, label = virksomhetsNavn),
                statistikkBransje.prosentLangTid(statistikkategori = BRANSJE, label = bransje.navn),
            ), // Virksomhet, Bransje
            trendTotalt = listOf(statistikkBransje.tilTrendTotalt(label = bransje.navn)), // Bransje
            tapteDagsverkTotalt = listOf(statistikkVirksomhet.tapteDagsverkTotalt(label = virksomhetsNavn)), // Virksomhet
            muligeDagsverkTotalt = listOf(statistikkVirksomhet.muligeDagsverkTotalt(label = virksomhetsNavn)), // Virksomhet
        )
    }
}

@Serializable
data class AggregertStatistikkDto(
    val statistikkategori: String,
    val label: String,
    val verdi: Double,
    val antallPersonerIBeregningen: Int,
    val kvartalerIBeregningen: List<KvartalIBeregning>,
) {
    companion object {
        // TODO: Alle disse utregningene er feil, kontroller
        // TODO: På sikt, flytt til ia-felles og ha det i en private/sealed klasse
        private fun List<Sykefraværsstatistikk>.beregnMuligeDagsverkTotalt() = this.sumOf { it.muligeDagsverk }

        private fun List<Sykefraværsstatistikk>.beregnTapteDagsverkTotalt() = this.sumOf { it.tapteDagsverk }

        private fun List<Sykefraværsstatistikk>.beregnProsentLangTid() = this.map { it.prosent }.average()

        private fun List<Sykefraværsstatistikk>.beregnProsentKortTid() = this.map { it.prosent }.average()

        private fun List<Sykefraværsstatistikk>.beregnProsentGradert() = this.map { it.prosent }.average()

        private fun List<Sykefraværsstatistikk>.beregnTrendTotalt() = -1.0

        fun List<Sykefraværsstatistikk>.prosentGradert(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = beregnProsentGradert(),
            antallPersonerIBeregningen = this.sumOf { it.antallPersoner },
            kvartalerIBeregningen = this.map { KvartalIBeregning(it.årstall, it.kvartal) },
        )

        fun List<Sykefraværsstatistikk>.prosentKortTid(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = beregnProsentKortTid(),
            antallPersonerIBeregningen = this.sumOf { it.antallPersoner },
            kvartalerIBeregningen = this.map { KvartalIBeregning(it.årstall, it.kvartal) },
        )

        fun List<Sykefraværsstatistikk>.prosentLangTid(
            statistikkategori: Statistikkategori,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = beregnProsentLangTid(),
            antallPersonerIBeregningen = this.sumOf { it.antallPersoner },
            kvartalerIBeregningen = this.map { KvartalIBeregning(it.årstall, it.kvartal) },
        )

        fun List<Sykefraværsstatistikk>.tilTrendTotalt(
            statistikkategori: Statistikkategori = BRANSJE,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = beregnTrendTotalt(),
            antallPersonerIBeregningen = this.sumOf { it.antallPersoner },
            kvartalerIBeregningen = this.map { KvartalIBeregning(it.årstall, it.kvartal) },
        )

        fun List<Sykefraværsstatistikk>.tapteDagsverkTotalt(
            statistikkategori: Statistikkategori = VIRKSOMHET,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = beregnTapteDagsverkTotalt(),
            antallPersonerIBeregningen = this.sumOf { it.antallPersoner },
            kvartalerIBeregningen = this.map { KvartalIBeregning(it.årstall, it.kvartal) },
        )

        fun List<Sykefraværsstatistikk>.muligeDagsverkTotalt(
            statistikkategori: Statistikkategori = VIRKSOMHET,
            label: String,
        ) = AggregertStatistikkDto(
            statistikkategori = statistikkategori.name,
            label = label,
            verdi = beregnMuligeDagsverkTotalt(),
            antallPersonerIBeregningen = this.sumOf { it.antallPersoner },
            kvartalerIBeregningen = this.tilKvartalIBeregning(),
        )
    }

    @Serializable
    data class KvartalIBeregning(
        val årstall: Int,
        val kvartal: Int,
    ) {
        companion object {
            fun List<Sykefraværsstatistikk>.tilKvartalIBeregning() = this.map { KvartalIBeregning(it.årstall, it.kvartal) }
        }
    }
}
