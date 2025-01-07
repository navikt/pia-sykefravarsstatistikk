package no.nav.pia.sykefravarsstatistikk.importering

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori

data class SykefraværsstatistikkImportKafkaMelding(
    val nøkkel: SykefraværsstatistikkImportKafkaMeldingNøkkel,
    val verdi: String,
)

@Serializable
data class SykefraværsstatistikkImportKafkaMeldingNøkkel(
    val kategori: Statistikkategori,
    val kode: String,
    val årstall: Int,
    val kvartal: Int,
)
