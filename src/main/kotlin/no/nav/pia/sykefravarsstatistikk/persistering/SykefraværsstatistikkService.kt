package no.nav.pia.sykefravarsstatistikk.persistering

import ia.felles.definisjoner.bransjer.Bransje
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkBransje
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkLand
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkNæring
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkSektor
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkVirksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
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
        orgnr: String,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkVirksomhet> {
        logger.info(
            "Henter statistikk for virksomhet med orgnr: '$orgnr' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}",
        )
        val sykefraværsstatistikkTilVirksomhet = sykefraværsstatistikkRepository.hentSykefraværsstatistikkVirksomhet(
            orgnr = orgnr,
        )
        return sykefraværsstatistikkTilVirksomhet.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkBransje(
        bransje: Bransje,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkBransje> {
        logger.info("Henter statistikk for bransje '$bransje' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilBransje = sykefraværsstatistikkRepository.hentSykefraværsstatistikkBransje(
            bransje = bransje,
        )
        return sykefraværsstatistikkTilBransje.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkNæring(
        næring: Næring,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkNæring> {
        logger.info("Henter statistikk for næring '$næring' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilNæring = sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(næring = næring)
        return sykefraværsstatistikkTilNæring.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkSektor(
        sektor: String,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<SykefraværsstatistikkSektor> {
        logger.info("Henter statistikk for sektor '$sektor' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilSektor = sykefraværsstatistikkRepository.hentSykefraværsstatistikkSektor(
            sektor = sektor,
        )
        return sykefraværsstatistikkTilSektor.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværsstatistikkLand(førsteÅrstalOgKvartal: ÅrstallOgKvartal): List<SykefraværsstatistikkLand> {
        logger.info("Henter statistikk for land  fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkLand = sykefraværsstatistikkRepository.hentSykefraværsstatistikkLand()
        return sykefraværsstatistikkLand.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }
}
