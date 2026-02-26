package no.nav.pia.sykefravarsstatistikk.persistering

import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class ImporttidspunktRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        val NÅVÆRENDE_KVARTAL = ÅrstallOgKvartal(årstall = 2025, kvartal = 4)
    }

    fun hentNyesteImporterteKvartal(): ÅrstallOgKvartal {
        logger.info("Henter nyeste importert kvartal, dette er hardkodet til ${NÅVÆRENDE_KVARTAL.årstall} Q${NÅVÆRENDE_KVARTAL.kvartal}.")
        return NÅVÆRENDE_KVARTAL
    }
}
