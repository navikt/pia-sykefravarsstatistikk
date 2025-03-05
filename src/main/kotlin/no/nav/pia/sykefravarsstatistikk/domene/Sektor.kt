package no.nav.pia.sykefravarsstatistikk.domene

import no.nav.pia.sykefravarsstatistikk.domene.Sektor.KOMMUNAL
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.PRIVAT
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.STATLIG
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.UKJENT

enum class Sektor(
    val kode: String,
    val beskrivelse: String,
) {
    STATLIG(kode = "1", beskrivelse = "Statlig forvaltning"),
    KOMMUNAL(kode = "2", beskrivelse = "Kommunal forvaltning"),
    PRIVAT(kode = "3", beskrivelse = "Privat og offentlig næringsvirksomhet"),
    UKJENT("0", "Ukjent sektor"),
}

fun String.tilSektor(): Sektor =
    when (this) {
        STATLIG.kode -> STATLIG
        STATLIG.name -> STATLIG
        KOMMUNAL.kode -> KOMMUNAL
        KOMMUNAL.name -> KOMMUNAL
        PRIVAT.kode -> PRIVAT
        PRIVAT.name -> PRIVAT
        else -> UKJENT
    }

fun String.erGyldigSektor(): Boolean = this.tilSektor() != UKJENT
