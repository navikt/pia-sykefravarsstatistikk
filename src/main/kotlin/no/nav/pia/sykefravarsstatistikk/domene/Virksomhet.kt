package no.nav.pia.sykefravarsstatistikk.domene

import ia.felles.definisjoner.bransjer.Bransje

interface Virksomhet {
    val orgnr: String
    val navn: String
    val næringskode: Næringskode

    fun bransje(): Bransje? = Bransje.fra(næringskode = næringskode.femsifferIdentifikator)
}
