package no.nav.pia.sykefravarsstatistikk.importering

import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SykefraværsstatistikkImportService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreSykefraværsstatistikk(sykefraværstatistikkDto: SykefraværsstatistikkDto) =
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(sykefraværsstatistikk = sykefraværstatistikkDto)

    fun lagreSykefraværsstatistikk(sykefraværsstatistikk: List<SykefraværsstatistikkDto>) {
        logger.info("Starter lagring av statistikk, antall statistikk som skal lagres: '${sykefraværsstatistikk.size}'")
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(sykefraværsstatistikk = sykefraværsstatistikk)
    }
}
