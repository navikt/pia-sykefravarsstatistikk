package no.nav.pia.sykefravarsstatistikk.domene

class OverordnetEnhet(
    override val orgnr: String,
    override val navn: String,
    override val næringskode: Næringskode,
    val antallAnsatte: Int,
) : Virksomhet
