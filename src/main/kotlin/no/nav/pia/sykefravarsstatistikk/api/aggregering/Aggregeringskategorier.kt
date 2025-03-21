package no.nav.pia.sykefravarsstatistikk.api.aggregering

import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import ia.felles.definisjoner.bransjer.Bransje as FellesBransje
import no.nav.pia.sykefravarsstatistikk.domene.Næring as DomeneNæring

sealed class Aggregeringskategorier {
    data object Land : Aggregeringskategorier()

    data class Næring(
        val næring: DomeneNæring,
    ) : Aggregeringskategorier()

    data class Bransje(
        val bransje: FellesBransje,
    ) : Aggregeringskategorier()

    data class Virksomhet(
        val virksomhet: Underenhet.Næringsdrivende,
    ) : Aggregeringskategorier()
}
