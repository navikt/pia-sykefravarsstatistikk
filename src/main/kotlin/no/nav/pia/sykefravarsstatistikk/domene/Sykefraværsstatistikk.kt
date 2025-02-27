package no.nav.pia.sykefravarsstatistikk.domene

sealed interface Sykefraværsstatistikk {
    val årstall: Int
    val kvartal: Int
    val antallPersoner: Int
    val tapteDagsverk: Double
    val muligeDagsverk: Double
    val prosent: Double
}
