package no.nav.pia.sykefravarsstatistikk.domene

sealed class Underenhet {
    abstract val orgnr: String
    abstract val navn: String
    abstract val overordnetEnhetOrgnr: String

    data class Næringsdrivende(
        override val orgnr: String,
        override val navn: String,
        override val overordnetEnhetOrgnr: String,
        override val næringskode: Næringskode,
        override val antallAnsatte: Int,
    ) : Underenhet(),
        Virksomhet

    data class IkkeNæringsdrivende(
        override val orgnr: String,
        override val navn: String,
        override val overordnetEnhetOrgnr: String,

        ) : Underenhet()
}
