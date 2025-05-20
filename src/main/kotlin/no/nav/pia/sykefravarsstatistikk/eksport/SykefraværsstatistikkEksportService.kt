package no.nav.pia.sykefravarsstatistikk.eksport

import ia.felles.definisjoner.bransjer.Bransje
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalBransje
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalLand
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæring
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalSektor
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalVirksomhet
import no.nav.pia.sykefravarsstatistikk.domene.tilSektor
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.persistering.BransjeSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.LandSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.NæringSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.NæringskodeSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SektorSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import no.nav.pia.sykefravarsstatistikk.persistering.VirksomhetSykefraværsstatistikkDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SykefraværsstatistikkEksportService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
    private val statistikkLandProdusent: SykefraværsstatistikkProducer,
    private val statistikkSektorProdusent: SykefraværsstatistikkProducer,
    private val statistikkNæringProdusent: SykefraværsstatistikkProducer,
    private val statistikkBransjeProdusent: SykefraværsstatistikkProducer,
//    private val statistikkNæringskodeProdusent: SykefraværsstatistikkProducer,
//    private val statistikkVirksomhetProdusent: SykefraværsstatistikkProducer,
//    private val statistikkVirksomhetGradertProdusent: SykefraværsstatistikkProducer,
//    private val statistikkMetadataVirksomhetProdusent: SykefraværsstatistikkProducer,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun eksporterSykefraværsstatistikk(sykefraværstatistikkDto: SykefraværsstatistikkDto) {
        val eksportkvartal = ÅrstallOgKvartal(
            årstall = sykefraværstatistikkDto.årstall,
            kvartal = sykefraværstatistikkDto.kvartal,
        )

        when (sykefraværstatistikkDto) {
            is LandSykefraværsstatistikkDto -> {
                eksporterSykefraværsstatistikkLand(
                    eksportkvartal = eksportkvartal,
                )
            }
            is SektorSykefraværsstatistikkDto -> {
                eksporterSykefraværsstatistikkSektor(
                    eksportkvartal = eksportkvartal,
                    sektor = sykefraværstatistikkDto.sektor.tilSektor(),
                )
            }
            is NæringSykefraværsstatistikkDto -> {
                eksporterSykefraværsstatistikkNæring(
                    eksportkvartal = eksportkvartal,
                    næring = Næring(sykefraværstatistikkDto.næring),
                )
            }
            is BransjeSykefraværsstatistikkDto -> {
                val bransjenavn = sykefraværstatistikkDto.bransje
                val bransje = Bransje.entries.firstOrNull { it.navn == bransjenavn }
                if (bransje == null) {
                    logger.error("Bransje $bransjenavn i db ikke funnet i domene, kjører ikke eksport")
                    return
                }

                eksporterSykefraværsstatistikkBransje(
                    eksportkvartal = eksportkvartal,
                    bransje = bransje,
                )
            }
            is NæringskodeSykefraværsstatistikkDto -> {
                eksporterSykefraværsstatistikkNæringskode(
                    eksportkvartal = eksportkvartal,
                )
            }
            is VirksomhetSykefraværsstatistikkDto -> {
                eksporterSykefraværsstatistikkVirksomhet(
                    eksportkvartal = eksportkvartal,
                )
            }
        }
    }

    private fun eksporterSykefraværsstatistikkLand(eksportkvartal: ÅrstallOgKvartal) {
        val statistikkategori = Statistikkategori.LAND
        logger.info("Eksporterer sykefraværsstatistikk for $statistikkategori - $eksportkvartal")
        val sykefraværsstatistikk = sykefraværsstatistikkRepository.hentSykefraværsstatistikkLand()
        val kode = sykefraværsstatistikk.first().land
        val statistikk = sykefraværsstatistikk.siste4Kvartaler(eksportkvartal)

        eksporterSykefraværsstatistikkPerKategori(
            eksportkvartal = eksportkvartal,
            kode = kode,
            statistikkategori = statistikkategori,
            statistikk = statistikk,
            produsent = statistikkLandProdusent,
        )
    }

    private fun eksporterSykefraværsstatistikkSektor(
        eksportkvartal: ÅrstallOgKvartal,
        sektor: Sektor,
    ) {
        val statistikkategori = Statistikkategori.SEKTOR
        logger.info("Eksporterer sykefraværsstatistikk for $statistikkategori - $eksportkvartal")
        val sykefraværsstatistikk = sykefraværsstatistikkRepository.hentSykefraværsstatistikkSektor(sektor = sektor)
        val kode = sykefraværsstatistikk.first().sektor
        // TODO: er dette det samme som sektor.kode? Hva er bedre, kode fra db eller kode fra input?
        val statistikk = sykefraværsstatistikk.siste4Kvartaler(eksportkvartal)

        eksporterSykefraværsstatistikkPerKategori(
            eksportkvartal = eksportkvartal,
            kode = kode,
            statistikkategori = statistikkategori,
            statistikk = statistikk,
            produsent = statistikkSektorProdusent,
        )
    }

    private fun eksporterSykefraværsstatistikkNæring(
        eksportkvartal: ÅrstallOgKvartal,
        næring: Næring,
    ) {
        val statistikkategori = Statistikkategori.NÆRING
        logger.info("Eksporterer sykefraværsstatistikk for $statistikkategori - $eksportkvartal")
        val sykefraværsstatistikk = sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(næring = næring)
        val kode = sykefraværsstatistikk.first().næring.tosifferIdentifikator
        // TODO: Sjekk om dette er rett kode
        val statistikk = sykefraværsstatistikk.siste4Kvartaler(eksportkvartal)

        eksporterSykefraværsstatistikkPerKategori(
            eksportkvartal = eksportkvartal,
            kode = kode,
            statistikkategori = statistikkategori,
            statistikk = statistikk,
            produsent = statistikkNæringProdusent,
        )
    }

    private fun eksporterSykefraværsstatistikkBransje(
        eksportkvartal: ÅrstallOgKvartal,
        bransje: Bransje,
    ) {
        val statistikkategori = Statistikkategori.BRANSJE
        logger.info("Eksporterer sykefraværsstatistikk for $statistikkategori - $eksportkvartal")
        val sykefraværsstatistikk = sykefraværsstatistikkRepository.hentSykefraværsstatistikkBransje(bransje = bransje)
        val kode = sykefraværsstatistikk.first().bransje.navn
        // TODO: Sjekk om dette er rett kode
        val statistikk = sykefraværsstatistikk.siste4Kvartaler(eksportkvartal)

        eksporterSykefraværsstatistikkPerKategori(
            eksportkvartal = eksportkvartal,
            kode = kode,
            statistikkategori = statistikkategori,
            statistikk = statistikk,
            produsent = statistikkBransjeProdusent,
        )
    }

    private fun eksporterSykefraværsstatistikkNæringskode(eksportkvartal: ÅrstallOgKvartal) {
        logger.warn("Eksport av sykefraværsstatistikk for næringskode ikke implementert")
    }

    private fun eksporterSykefraværsstatistikkVirksomhet(eksportkvartal: ÅrstallOgKvartal) {
        logger.warn("Eksport av sykefraværsstatistikk for virksomhet ikke implementert")
    }

    private fun eksporterSykefraværsstatistikkPerKategori(
        eksportkvartal: ÅrstallOgKvartal,
        statistikkategori: Statistikkategori,
        kode: String,
        statistikk: List<Sykefraværsstatistikk>,
        produsent: SykefraværsstatistikkProducer,
    ) {
        val umaskertSykefraværsstatistikk = statistikk.tilUmaskertSykefraværUtenProsentForEttKvartal()
        val sykefraværMedKategoriSisteKvartal = umaskertSykefraværsstatistikk.max()
            .tilSykefraværMedKategori(statistikkategori, kode)

        if (sykefraværMedKategoriSisteKvartal.årstallOgKvartal != eksportkvartal) {
            logger.warn("Siste kvartal i uthentet statistikk er ikke samme som inneværende kvartal")
            return
        }

        val statistikkategoriKafkamelding = when (statistikkategori) {
            Statistikkategori.LAND,
            Statistikkategori.SEKTOR,
            Statistikkategori.NÆRING,
            Statistikkategori.BRANSJE,
            Statistikkategori.OVERORDNET_ENHET,
            Statistikkategori.NÆRINGSKODE,
            Statistikkategori.VIRKSOMHET,
            -> StatistikkategoriKafkamelding(
                sisteKvartal = sykefraværMedKategoriSisteKvartal,
                siste4Kvartal = SykefraværFlereKvartalerForEksport(umaskertSykefravær = umaskertSykefraværsstatistikk),
            )
        }
        produsent.sendPåKafka(statistikkategoriKafkamelding)
        logger.info(
            "Melding eksportert på Kafka for statistikkategori ${statistikkategori.name}, ${umaskertSykefraværsstatistikk.size} kvartaler fram til $eksportkvartal.",
        )
    }

    private fun List<Sykefraværsstatistikk>.siste4Kvartaler(eksportkvartal: ÅrstallOgKvartal): List<Sykefraværsstatistikk> {
        val førsteKvartal: ÅrstallOgKvartal = eksportkvartal.minusKvartaler(3)
        return this.filter {
            it.årstallOgKvartal() >= førsteKvartal && it.årstallOgKvartal() <= eksportkvartal
        }
    }
}

// TODO: Se gjennom om dette blir rett, vi må kanskje endre på denne litt
// TODO: rename 'UmaskertSykefraværForEttKvartal' da graderingsprosent og sykefraværsprosent er to forskjellige ting
//  og kalkuleres på to forskjellige måter
//  -> sykefraværsprosent = (antall tapte dagsverk / antall mulige dagsverk) * 100 -- lav prosent er bra
//  -> graderingsprosent = (antall tapte graderte dagsverk / antall tapte dagsverk) * 100 -- høy prosent er bra
fun List<Sykefraværsstatistikk>.tilUmaskertSykefraværUtenProsentForEttKvartal(): List<UmaskertSykefraværUtenProsentForEttKvartal> =
    this.map {
        when (it) {
            is UmaskertSykefraværsstatistikkForEttKvartalLand,
            is UmaskertSykefraværsstatistikkForEttKvartalSektor,
            is UmaskertSykefraværsstatistikkForEttKvartalNæring,
//                is SykefraværsstatistikkNæringMedVarighet, //TODO: mangler i ny app ?
            is UmaskertSykefraværsstatistikkForEttKvartalBransje,
//                is SykefraværsstatistikkForNæringskode, //TODO: mangler i ny app ?
            is UmaskertSykefraværsstatistikkForEttKvartalVirksomhet,
//                is SykefraværsstatistikkVirksomhetUtenVarighet, //TODO: mangler i ny app ?
            -> UmaskertSykefraværUtenProsentForEttKvartal(
                årstallOgKvartal = ÅrstallOgKvartal(it.årstall, it.kvartal),
                dagsverkTeller = it.tapteDagsverk,
                dagsverkNevner = it.muligeDagsverk,
                antallPersoner = it.antallPersoner,
            )
        }
    }
