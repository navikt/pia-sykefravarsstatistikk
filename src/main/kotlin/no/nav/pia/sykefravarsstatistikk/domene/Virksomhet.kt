package no.nav.pia.sykefravarsstatistikk.domene

interface Virksomhet {
    val orgnr: String
    val navn: String
    val næringskode: Næringskode
}
