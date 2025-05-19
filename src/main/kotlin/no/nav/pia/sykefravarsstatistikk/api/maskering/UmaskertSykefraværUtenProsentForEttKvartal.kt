package no.nav.pia.sykefravarsstatistikk.api.maskering

import arrow.core.Either
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.exceptions.Statistikkfeil
import java.math.BigDecimal
import java.math.RoundingMode

// OBS: denne var UmaskertSykefraværForEttKvartal i sykefraværsstatistikk-api
open class UmaskertSykefraværUtenProsentForEttKvartal(
    open val årstallOgKvartal: ÅrstallOgKvartal,
    dagsverkTeller: BigDecimal,
    dagsverkNevner: BigDecimal,
    open val antallPersoner: Int,
) : Comparable<UmaskertSykefraværUtenProsentForEttKvartal> {
    val dagsverkTeller: BigDecimal = dagsverkTeller.setScale(1, RoundingMode.HALF_UP)
    val dagsverkNevner: BigDecimal = dagsverkNevner.setScale(1, RoundingMode.HALF_UP)

    constructor(statistikk: Sykefraværsstatistikk) : this(
        ÅrstallOgKvartal(statistikk.årstall, statistikk.kvartal),
        statistikk.tapteDagsverk,
        statistikk.muligeDagsverk,
        statistikk.antallPersoner,
    )

    fun tilSykefraværMedKategori(
        kategori: Statistikkategori,
        kode: String,
    ): SykefraværMedKategori =
        SykefraværMedKategori(
            statistikkategori = kategori,
            kode = kode,
            årstallOgKvartal = årstallOgKvartal,
            tapteDagsverk = dagsverkTeller,
            muligeDagsverk = dagsverkNevner,
            antallPersoner = antallPersoner,
        )

    fun kalkulerSykefraværsprosent(): Either<Statistikkfeil, BigDecimal> =
        StatistikkUtils.kalkulerSykefraværsprosent(dagsverkTeller, dagsverkNevner)

    override fun compareTo(other: UmaskertSykefraværUtenProsentForEttKvartal) = compareValuesBy(this, other) { it.årstallOgKvartal }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UmaskertSykefraværUtenProsentForEttKvartal

        if (dagsverkTeller != other.dagsverkTeller) return false
        if (dagsverkNevner != other.dagsverkNevner) return false
        if (antallPersoner != other.antallPersoner) return false
        if (årstallOgKvartal != other.årstallOgKvartal) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dagsverkTeller.hashCode()
        result = 31 * result + dagsverkNevner.hashCode()
        result = 31 * result + antallPersoner
        result = 31 * result + årstallOgKvartal.hashCode()
        return result
    }

    override fun toString(): String =
        "UmaskertSykefraværUtenProsentForEttKvartal(årstallOgKvartal=$årstallOgKvartal, " +
            "dagsverkTeller=$dagsverkTeller, dagsverkNevner=$dagsverkNevner, " +
            "antallPersoner=$antallPersoner)"
}
