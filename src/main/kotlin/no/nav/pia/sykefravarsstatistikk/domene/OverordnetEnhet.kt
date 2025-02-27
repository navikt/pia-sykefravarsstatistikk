package no.nav.pia.sykefravarsstatistikk.domene

import ia.felles.definisjoner.bransjer.Bransje

class OverordnetEnhet(
    override val orgnr: String,
    override val navn: String,
    override val næringskode: Næringskode,
    val antallAnsatte: Int,
) : Virksomhet {
    fun bransje(): Bransje? = Bransje.fra(næringskode = næringskode.femsifferIdentifikator)
}
