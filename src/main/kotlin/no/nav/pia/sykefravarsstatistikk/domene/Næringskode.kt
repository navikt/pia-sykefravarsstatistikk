package no.nav.pia.sykefravarsstatistikk.domene

class Næringskode(
    val femsifferIdentifikator: String,
) {
    companion object {
        private fun tilFemsiffer(kode: String?): Næringskode? =
            if (kode != null && (kode.matches(Regex("""^\d{2}\.\d{3}$""")) || kode.matches(Regex("""^\d{5}$"""))))
                Næringskode(kode.replace(".", ""))
            else
                null

        fun BrregNæringskodeDto.tilDomene(): Næringskode? = tilFemsiffer(this.kode)

        fun String.tilNæringskode(): Næringskode? = tilFemsiffer(this)
    }

    val næring = Næring(tosifferIdentifikator = femsifferIdentifikator.take(2))
}
