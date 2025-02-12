package no.nav.pia.sykefravarsstatistikk.domene

import java.math.BigDecimal

sealed interface Sykefraværsstatistikk {
    val årstall: Int
    val kvartal: Int
    val antallPersoner: Int
    val tapteDagsverk: BigDecimal?
    val muligeDagsverk: BigDecimal?
}
