package no.nav.pia.sykefravarsstatistikk.domene

// TODO: Flytt til ia-felles
class Næringskode private constructor(
    val femsifferIdentifikator: String,
) {
    companion object Factory {
        fun tilFemsiffer(kode: String): Næringskode = Næringskode(kode.replace(".", ""))
    }

    init {
        require(femsifferIdentifikator.matches("""\d{5}""".toRegex())) {
            "Næringskode skal være 5 siffer, men var $femsifferIdentifikator"
        }
    }

    val næring = Næring(femsifferIdentifikator.take(2))
}
