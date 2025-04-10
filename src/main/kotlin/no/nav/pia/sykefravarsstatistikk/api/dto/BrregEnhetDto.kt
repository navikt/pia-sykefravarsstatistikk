package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregInstitusjonellSektorkodeDto.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.domene.BrregNæringskodeDto
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet

@Serializable
data class BrregEnhetDto(
    val organisasjonsnummer: String,
    val navn: String,
    val naeringskode1: BrregNæringskodeDto,
    val overordnetEnhet: String? = null,
    val antallAnsatte: Int = 0,
    val institusjonellSektorkode: BrregInstitusjonellSektorkodeDto? = null,
) {
    fun tilDomene(): OverordnetEnhet =
        OverordnetEnhet(
            orgnr = this.organisasjonsnummer,
            navn = this.navn,
            næringskode = this.naeringskode1.tilDomene(),
            antallAnsatte = this.antallAnsatte,
            sektor = this.institusjonellSektorkode?.tilDomene(),
        )
}
