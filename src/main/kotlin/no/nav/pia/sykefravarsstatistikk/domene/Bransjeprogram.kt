package no.nav.pia.sykefravarsstatistikk.domene

import ia.felles.definisjoner.bransjer.Bransje
import ia.felles.definisjoner.bransjer.BransjeId

object Bransjeprogram {
    fun finnBransje(næringskode: Næringskode): Bransje? =
        Bransje.entries.firstOrNull { bransje ->
            bransje.bransjeId.let {
                when (it) {
                    is BransjeId.Næring -> næringskode.næring.tosifferIdentifikator == it.næring
                    is BransjeId.Næringskoder -> it.næringskoder.contains(næringskode.femsifferIdentifikator)
                }
            }
        }
}
