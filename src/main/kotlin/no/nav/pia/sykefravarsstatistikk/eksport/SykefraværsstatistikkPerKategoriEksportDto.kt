package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori

@Serializable
data class SykefraværsstatistikkPerKategoriEksportDto(
    val kategori: Statistikkategori,
    val kode: String,
    val sistePubliserteKvartal: SistePubliserteKvartalDto,
    val siste4Kvartal: Siste4KvartalDto,
)

fun StatistikkategoriKafkamelding.tilDto(): SykefraværsstatistikkPerKategoriEksportDto =
    SykefraværsstatistikkPerKategoriEksportDto(
        kategori = sisteKvartal.kategori,
        kode = sisteKvartal.kode,
        sistePubliserteKvartal = sisteKvartal.tilDto(),
        siste4Kvartal = siste4Kvartal.tilDto(),
    )
