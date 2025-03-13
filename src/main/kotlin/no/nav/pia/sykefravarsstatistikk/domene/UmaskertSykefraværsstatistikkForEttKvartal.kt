package no.nav.pia.sykefravarsstatistikk.domene

import ia.felles.definisjoner.bransjer.Bransje
import java.math.BigDecimal
import java.time.LocalDateTime

data class UmaskertSykefraværsstatistikkForEttKvartalLand(
    val land: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: BigDecimal,
    override val muligeDagsverk: BigDecimal,
    override val prosent: BigDecimal,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalSektor(
    val sektor: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: BigDecimal,
    override val muligeDagsverk: BigDecimal,
    override val prosent: BigDecimal,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalNæring(
    val næring: Næring,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: BigDecimal,
    override val muligeDagsverk: BigDecimal,
    override val prosent: BigDecimal,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalBransje(
    val bransje: Bransje,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: BigDecimal,
    override val muligeDagsverk: BigDecimal,
    override val prosent: BigDecimal,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk

data class UmaskertSykefraværsstatistikkForEttKvartalVirksomhet(
    val orgnr: String,
    override val årstall: Int,
    override val kvartal: Int,
    override val antallPersoner: Int,
    override val tapteDagsverk: BigDecimal,
    override val muligeDagsverk: BigDecimal,
    override val prosent: BigDecimal,
    val tapteDagsverkGradert: BigDecimal,
    val rectype: String,
    val opprettet: LocalDateTime,
) : Sykefraværsstatistikk
