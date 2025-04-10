package no.nav.pia.sykefravarsstatistikk.domene

class OverordnetEnhet(
    override val orgnr: String,
    override val navn: String,
    override val næringskode: Næringskode,
    override val antallAnsatte: Int,
    val sektor: Sektor? = null,
) : Virksomhet
