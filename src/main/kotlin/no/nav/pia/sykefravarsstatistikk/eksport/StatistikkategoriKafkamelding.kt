package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.api.maskering.SykefraværMedKategori

data class StatistikkategoriKafkamelding(
    val sisteKvartal: SykefraværMedKategori,
    val siste4Kvartal: SykefraværFlereKvartalerForEksport,
)
