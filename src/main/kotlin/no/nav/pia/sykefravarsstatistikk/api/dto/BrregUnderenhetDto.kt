package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.BrregNæringskodeDto
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet

@Serializable
data class BrregUnderenhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val naeringskode1: BrregNæringskodeDto,
    val overordnetEnhet: String,
    val antallAnsatte: Int = 0,
) {
    fun tilDomene(): Underenhet =
        if (naeringskode1 != null) {
            Underenhet.Næringsdrivende(
                orgnr = organisasjonsnummer,
                overordnetEnhetOrgnr = overordnetEnhet,
                navn = navn,
                næringskode = naeringskode1.tilDomene(),
                antallAnsatte = antallAnsatte,
            )
        } else {
            Underenhet.IkkeNæringsdrivende(
                orgnr = organisasjonsnummer,
            )
        }
}
