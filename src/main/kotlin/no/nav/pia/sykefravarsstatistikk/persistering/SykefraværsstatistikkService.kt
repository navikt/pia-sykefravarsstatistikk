package no.nav.pia.sykefravarsstatistikk.persistering

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SykefraværsstatistikkService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreSykefraværsstatistikk(statistikk: List<SykefraværsstatistikkDto>) {
        logger.info("Starter import av statistikk, antall statistikk som skal lagres: '${statistikk.size}'")
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(statistikk)
    }

    fun lagreVirksomhetMetadata(metadata: List<VirksomhetMetadataDto>) {
        logger.info("Starter import av metadata, antall metadata som skal lagres: '${metadata.size}'\")")
        sykefraværsstatistikkRepository.insertVirksomhetMetadata(metadata)
    }
}
