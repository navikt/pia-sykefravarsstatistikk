package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalBransje
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalLand
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæring
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalSektor
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalVirksomhet
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
    private val statistikkNæringskodeProdusent: SykefraværsstatistikkProducer,
    private val statistikkVirksomhetProdusent: SykefraværsstatistikkProducer,
    private val statistikkVirksomhetGradertProdusent: SykefraværsstatistikkProducer,
    private val statistikkMetadataVirksomhetProdusent: SykefraværsstatistikkProducer,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun eksporterSykefraværsstatistikk(sykefraværstatistikkDto: SykefraværsstatistikkDto) {
        val inneværendeKvartal = ÅrstallOgKvartal(
            årstall = sykefraværstatistikkDto.årstall,
            kvartal = sykefraværstatistikkDto.kvartal,
        )
        val førsteKvartal: ÅrstallOgKvartal = inneværendeKvartal.minusKvartaler(3)

        when (sykefraværstatistikkDto) {
            is LandSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkLand(
                førsteKvartal = førsteKvartal,
                eksportkvartal = inneværendeKvartal,
            )
            is SektorSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkSektor(
                førsteKvartal = førsteKvartal,
                inneværendeKvartal = inneværendeKvartal,
            )
            is NæringSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkNæring(
                førsteKvartal = førsteKvartal,
                inneværendeKvartal = inneværendeKvartal,
            )
            is BransjeSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkBransje(
                førsteKvartal = førsteKvartal,
                inneværendeKvartal = inneværendeKvartal,
            )
            is NæringskodeSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkNæringskode(
                førsteKvartal = førsteKvartal,
                inneværendeKvartal = inneværendeKvartal,
            )
            is VirksomhetSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkVirksomhet(
                førsteKvartal = førsteKvartal,
                inneværendeKvartal = inneværendeKvartal,
            )
        }
    }

    private fun eksporterSykefraværsstatistikkLand(
        førsteKvartal: ÅrstallOgKvartal,
        eksportkvartal: ÅrstallOgKvartal,
    ) {
        val sykefraværsstatistikk = sykefraværsstatistikkRepository.hentSykefraværsstatistikkLand()
        val statistikkSiste4Kvartaler = sykefraværsstatistikk.filter {
            it.årstallOgKvartal() >= førsteKvartal && it.årstallOgKvartal() <= eksportkvartal
        }

        logger.info("Eksporterer sykefraværsstatistikk for ${Statistikkategori.LAND} fra $førsteKvartal til $eksportkvartal")

        eksporterSykefraværsstatistikkPerKategori(
            eksportkvartal = eksportkvartal,
            kode = sykefraværsstatistikk.first().land,
            statistikkategori = Statistikkategori.LAND,
            statistikk = statistikkSiste4Kvartaler,
            produsent = statistikkLandProdusent,
        )
    }

    private fun eksporterSykefraværsstatistikkSektor(
        førsteKvartal: ÅrstallOgKvartal,
        inneværendeKvartal: ÅrstallOgKvartal,
    ) {
        logger.warn("Eksport av sykefraværsstatistikk for sektor ikke implementert")
    }

    private fun eksporterSykefraværsstatistikkNæring(
        førsteKvartal: ÅrstallOgKvartal,
        inneværendeKvartal: ÅrstallOgKvartal,
    ) {
        logger.warn("Eksport av sykefraværsstatistikk for næring ikke implementert")
    }

    private fun eksporterSykefraværsstatistikkBransje(
        førsteKvartal: ÅrstallOgKvartal,
        inneværendeKvartal: ÅrstallOgKvartal,
    ) {
        logger.warn("Eksport av sykefraværsstatistikk for bransje ikke implementert")
    }

    private fun eksporterSykefraværsstatistikkNæringskode(
        førsteKvartal: ÅrstallOgKvartal,
        inneværendeKvartal: ÅrstallOgKvartal,
    ) {
        logger.warn("Eksport av sykefraværsstatistikk for næringskode ikke implementert")
    }

    private fun eksporterSykefraværsstatistikkVirksomhet(
        førsteKvartal: ÅrstallOgKvartal,
        inneværendeKvartal: ÅrstallOgKvartal,
    ) {
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
