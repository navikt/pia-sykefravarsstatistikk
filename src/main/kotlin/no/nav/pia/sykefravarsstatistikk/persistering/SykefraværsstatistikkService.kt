package no.nav.pia.sykefravarsstatistikk.persistering

import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkVirksomhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SykefraværsstatistikkService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreSykefraværsstatistikk(statistikk: List<SykefraværsstatistikkDto>) {
        logger.info("Starter lagring av statistikk, antall statistikk som skal lagres: '${statistikk.size}'")
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(statistikk)
    }

    fun hentSykefraværsstatistikkVirksomhet(
        fnr: String,
        orgnr: String,
    ): List<SykefraværsstatistikkVirksomhet> = emptyList()
}
