package no.nav.pia.sykefravarsstatistikk.domene

import ia.felles.definisjoner.bransjer.Bransje

interface Virksomhet {
    val orgnr: String
    val navn: String
    val næringskode: Næringskode?
    val antallAnsatte: Int

    fun bransje(): Bransje? = næringskode?.let { Bransje.fra(næringskode = næringskode!!.femsifferIdentifikator)}
}
