package no.nav.pia.sykefravarsstatistikk.eksport

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori

@Serializable
data class GradertSykemeldingEksportDto(
    val kategori: Statistikkategori,
    val kode: String,
    val sistePubliserteKvartal: GraderingSistePubliserteKvartal,
    val siste4Kvartal: GraderingSiste4Kvartal,
)

fun GradertStatistikkategoriKafkamelding.tilGradertDto(): GradertSykemeldingEksportDto =
    GradertSykemeldingEksportDto(
        kategori = sisteKvartal.kategori,
        kode = sisteKvartal.kode,
        sistePubliserteKvartal = sisteKvartal.tilGradertDto(),
        siste4Kvartal = siste4Kvartal.tilGradertDto(),
    )
