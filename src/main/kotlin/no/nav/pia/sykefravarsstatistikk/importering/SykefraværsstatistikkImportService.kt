package no.nav.pia.sykefravarsstatistikk.importering

import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository

class SykefraværsstatistikkImportService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    fun lagreSykefraværsstatistikk(sykefraværstatistikkDto: SykefraværsstatistikkDto) =
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(sykefraværsstatistikk = sykefraværstatistikkDto)
}
