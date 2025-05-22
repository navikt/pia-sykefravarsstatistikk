package no.nav.pia.sykefravarsstatistikk.eksport

interface Kafkamelding {
    val nøkkel: String
    val innhold: String
}
