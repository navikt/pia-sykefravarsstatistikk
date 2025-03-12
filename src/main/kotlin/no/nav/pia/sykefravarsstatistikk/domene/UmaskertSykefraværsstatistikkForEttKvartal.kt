package no.nav.pia.sykefravarsstatistikk.domene

import ia.felles.definisjoner.bransjer.Bransje
import java.time.LocalDateTime

data class UmaskertSykefraværsstatistikkForEttKvartalLand(
    val land: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalSektor(
    val sektor: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalNæring(
    val næring: Næring,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalBransje(
    val bransje: Bransje,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: Double,
    override val muligeDagsverk: Double,
    override val prosent: Double,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalVirksomhet(
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
