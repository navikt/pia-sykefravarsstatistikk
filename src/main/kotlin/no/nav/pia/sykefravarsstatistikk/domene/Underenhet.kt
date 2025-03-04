package no.nav.pia.sykefravarsstatistikk.domene

class Underenhet(
    override val orgnr: String,
    override val navn: String,
    override val næringskode: Næringskode,
    val overordnetEnhetOrgnr: String,
    val antallAnsatte: Int,
) : Virksomhet
