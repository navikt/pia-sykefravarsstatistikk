package no.nav.pia.sykefravarsstatistikk.importering

import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue

class SykefraværsstatistikkImportService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreSykefraværsstatistikk(sykefraværstatistikkDto: SykefraværsstatistikkDto) =
        sykefraværsstatistikkRepository.insertSykefraværsstatistikk(sykefraværsstatistikk = sykefraværstatistikkDto)

    fun lagreSykefraværsstatistikk(sykefraværsstatistikk: List<SykefraværsstatistikkDto>): Long {
        logger.info("Starter lagring av statistikk, antall statistikk som skal lagres: '${sykefraværsstatistikk.size}'")
        val timedValue = measureTimedValue {
            sykefraværsstatistikkRepository.insertSykefraværsstatistikk(sykefraværsstatistikk = sykefraværsstatistikk)
        }
        val snitt = timedValue.duration.inWholeMicroseconds / sykefraværsstatistikk.size
        logger.info(
            "Lagring av statistikk fullført, antall statistikk som er lagret: '${sykefraværsstatistikk.size}', " +
                "Tid brukt i millisekunder: '${timedValue.duration.inWholeMicroseconds}, tid brukt per statistikk: '$snitt' mikrosekunder'",
        )
        return snitt
    }
}
