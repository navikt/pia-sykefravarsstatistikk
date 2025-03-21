package no.nav.pia.sykefravarsstatistikk.api.maskering

import no.nav.pia.sykefravarsstatistikk.api.aggregering.SykefraværForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import java.math.BigDecimal
import java.util.Objects

class SykefraværMedKategori(
    @JvmField val kode: String,
    statistikkategori: Statistikkategori,
    årstallOgKvartal: ÅrstallOgKvartal?,
    tapteDagsverk: BigDecimal?,
    muligeDagsverk: BigDecimal?,
    override val antallPersoner: Int
) : SykefraværForEttKvartal(årstallOgKvartal, tapteDagsverk, muligeDagsverk, antallPersoner) {
    val kategori: Statistikkategori = statistikkategori

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SykefraværMedKategori) return false
        if (!super.equals(other)) return false
        return (antallPersoner == other.antallPersoner) && (kategori == other.kategori) && (kode == other.kode)
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), kategori, kode, antallPersoner)
    }
}
