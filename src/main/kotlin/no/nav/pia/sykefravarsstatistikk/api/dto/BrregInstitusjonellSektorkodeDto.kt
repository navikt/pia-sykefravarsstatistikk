package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.KOMMUNAL
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.PRIVAT
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.STATLIG
import no.nav.pia.sykefravarsstatistikk.domene.Sektor.UKJENT

@Serializable
data class BrregInstitusjonellSektorkodeDto(
    val kode: String,
    val beskrivelse: String,
) {
    companion object {
        fun BrregInstitusjonellSektorkodeDto.tilDomene(): Sektor =
            when (this.kode) {
                "1110" -> PRIVAT
                "1120" -> PRIVAT
                "1510" -> PRIVAT
                "1520" -> PRIVAT
                "2100" -> PRIVAT
                "2300" -> PRIVAT
                "2500" -> PRIVAT
                "3100" -> STATLIG
                "3200" -> PRIVAT
                "3500" -> PRIVAT
                "3600" -> PRIVAT
                "3900" -> STATLIG
                "4100" -> PRIVAT
                "4300" -> PRIVAT
                "4500" -> PRIVAT
                "4900" -> PRIVAT
                "5500" -> PRIVAT
                "5700" -> PRIVAT
                "6100" -> STATLIG
                "6500" -> KOMMUNAL
                "7000" -> PRIVAT
                "8200" -> PRIVAT
                "8300" -> PRIVAT
                "8500" -> PRIVAT
                "9000" -> PRIVAT
                else -> UKJENT
            }
    }
}
