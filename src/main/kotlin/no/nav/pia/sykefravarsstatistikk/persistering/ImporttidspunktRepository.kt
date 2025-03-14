package no.nav.pia.sykefravarsstatistikk.persistering

import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class ImporttidspunktRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentNyesteImporterteKvartal(): ÅrstallOgKvartal {
        // TODO: Hent fra db?
        logger.info("Henter nyeste importert kvartal, dette er hardkodet til 2024K4")
        return ÅrstallOgKvartal(årstall = 2024, kvartal = 4)
    }
}
