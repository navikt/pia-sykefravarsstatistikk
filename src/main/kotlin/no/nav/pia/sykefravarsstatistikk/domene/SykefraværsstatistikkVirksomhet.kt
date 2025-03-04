package no.nav.pia.sykefravarsstatistikk.domene

import java.time.LocalDateTime

data class SykefraværsstatistikkLand(
    val land: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class SykefraværsstatistikkSektor(
    val sektor: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class SykefraværsstatistikkNæring(
    val næring: Næring,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class SykefraværsstatistikkBransje(
    val bransje: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class SykefraværsstatistikkVirksomhet(
    val orgnr: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val tapteDagsverkGradert: Double,
    val rectype: String,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk
