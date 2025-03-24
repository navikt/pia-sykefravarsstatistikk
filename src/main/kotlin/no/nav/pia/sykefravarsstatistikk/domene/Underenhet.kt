package no.nav.pia.sykefravarsstatistikk.domene

sealed class Underenhet {
    abstract val orgnr: String

    data class Næringsdrivende(
        override val orgnr: String,
        val overordnetEnhetOrgnr: String,
        override val navn: String,
        override val næringskode: Næringskode,
        override val antallAnsatte: Int,
    ) : Underenhet(),
        Virksomhet

    data class IkkeNæringsdrivende(
        override val orgnr: String,
    ) : Underenhet()
}
