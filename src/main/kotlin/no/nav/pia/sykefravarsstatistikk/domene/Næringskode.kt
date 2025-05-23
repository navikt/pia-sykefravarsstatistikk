package no.nav.pia.sykefravarsstatistikk.domene

class Næringskode private constructor(
    val femsifferIdentifikator: String,
) {
    companion object {
        private fun tilFemsiffer(kode: String): Næringskode = Næringskode(kode.replace(".", ""))

        fun BrregNæringskodeDto.tilDomene(): Næringskode = tilFemsiffer(this.kode)

        fun String.tilNæringskode(): Næringskode = tilFemsiffer(this)
    }

    init {
        require(femsifferIdentifikator.matches("""\d{5}""".toRegex())) {
            "Næringskode skal være 5 siffer, men var $femsifferIdentifikator"
        }
    }

    val næring = Næring(tosifferIdentifikator = femsifferIdentifikator.take(2))
}
