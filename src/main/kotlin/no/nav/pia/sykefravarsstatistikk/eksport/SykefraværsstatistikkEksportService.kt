package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.persistering.BransjeSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.LandSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.NæringSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.NæringskodeSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SektorSykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkDto
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository
import no.nav.pia.sykefravarsstatistikk.persistering.VirksomhetSykefraværsstatistikkDto

class SykefraværsstatistikkEksportService(
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    fun eksporterSykefraværsstatistikk(sykefraværstatistikkDto: SykefraværsstatistikkDto) {
        when (sykefraværstatistikkDto) {
            is LandSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkLand(sykefraværstatistikkDto)
            is SektorSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkSektor(sykefraværstatistikkDto)
            is NæringSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkNæring(sykefraværstatistikkDto)
            is BransjeSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkBransje(sykefraværstatistikkDto)
            is NæringskodeSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkNæringskode(sykefraværstatistikkDto)
            is VirksomhetSykefraværsstatistikkDto -> eksporterSykefraværsstatistikkVirksomhet(sykefraværstatistikkDto)
        }
    }

    private fun eksporterSykefraværsstatistikkLand(sykefraværstatistikkDto: LandSykefraværsstatistikkDto) {
//                val resultat = sykefraværsstatistikkEksportService.hent4Kvartaler(sykefraværstatistikkDto = sykefraværstatistikkDto)
//                private fun eksporterSykefraværsstatistikkLand(årstallOgKvartal: ÅrstallOgKvartal) {
//                    sykefraværStatistikkLandRepository.hentSykefraværstatistikkLand(årstallOgKvartal inkludertTidligere 3)
//                        .groupBy({ "NO" }, { it }).let {
//                            eksporterSykefraværsstatistikkPerKategori(
//                                eksportkvartal = årstallOgKvartal,
//                                sykefraværGruppertEtterKode = it,
//                                statistikkategori = Statistikkategori.LAND,
//                                kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_LAND_V1
//                            )
//                        }
//                }
    }

    private fun eksporterSykefraværsstatistikkSektor(sykefraværstatistikkDto: SektorSykefraværsstatistikkDto) {
//        private fun eksporterSykefraværsstatistikkSektor(årstallOgKvartal: ÅrstallOgKvartal) {
//            sykefraværStatistikkSektorRepository.hentForKvartaler(årstallOgKvartal inkludertTidligere 3)
//                .groupBy({ it.sektor.name }, { it })
//                .let {
//                    eksporterSykefraværsstatistikkPerKategori(
//                        eksportkvartal = årstallOgKvartal,
//                        sykefraværGruppertEtterKode = it,
//                        statistikkategori = Statistikkategori.SEKTOR,
//                        kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_SEKTOR_V1
//                    )
//                }
//        }
    }

    private fun eksporterSykefraværsstatistikkNæring(sykefraværstatistikkDto: NæringSykefraværsstatistikkDto) {
//                private fun eksporterSykefraværsstatistikkNæring(årstallOgKvartal: ÅrstallOgKvartal) {
//                    sykefraværStatistikkNæringRepository.hentForAlleNæringer(
//                        årstallOgKvartal inkludertTidligere 3
//                    ).groupBy({ it.næring }, { it }).let {
//                        eksporterSykefraværsstatistikkPerKategori(
//                            eksportkvartal = årstallOgKvartal,
//                            sykefraværGruppertEtterKode = it,
//                            statistikkategori = Statistikkategori.NÆRING,
//                            kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_NARING_V1
//                        )
//                    }
//                }
        // TODO: Send resultat på kafka som DTO
    }

    private fun eksporterSykefraværsstatistikkBransje(sykefraværstatistikkDto: BransjeSykefraværsstatistikkDto) {
//                private fun eksporterSykefraværsstatistikkBransje(kvartal: ÅrstallOgKvartal) {
//                    hentSykefraværsstatistikkForBransje(
//                        kvartaler = kvartal inkludertTidligere 3,
//                        sykefraværsstatistikkNæringRepository = sykefraværStatistikkNæringRepository,
//                        sykefraværStatistikkNæringskodeRepository = sykefraværStatistikkNæringskodeRepository
//                    )
//                        .groupBy({ it.bransje.name }, { it }).let {
//                            eksporterSykefraværsstatistikkPerKategori(
//                                eksportkvartal = kvartal,
//                                sykefraværGruppertEtterKode = it,
//                                statistikkategori = Statistikkategori.BRANSJE,
//                                kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_BRANSJE_V1
//                            )
//                        }
//                }
    }

    private fun eksporterSykefraværsstatistikkNæringskode(sykefraværstatistikkDto: NæringskodeSykefraværsstatistikkDto) {
//                private fun eksporterSykefraværsstatistikkNæringskode(årstallOgKvartal: ÅrstallOgKvartal) {
//                    sykefraværStatistikkNæringskodeRepository.hentAltForKvartaler(årstallOgKvartal inkludertTidligere 3)
//                        .groupBy({ it.næringskode }, { it }).let {
//                            eksporterSykefraværsstatistikkPerKategori(
//                                eksportkvartal = årstallOgKvartal,
//                                sykefraværGruppertEtterKode = it,
//                                statistikkategori = Statistikkategori.NÆRINGSKODE,
//                                kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_NARINGSKODE_V1
//                            )
//                        }
//                }
        // TODO: Send resultat på kafka som DTO
    }

    private fun eksporterSykefraværsstatistikkVirksomhet(sykefraværstatistikkDto: VirksomhetSykefraværsstatistikkDto) {
        // TODO: Hvordan håndtere virksomhet_gradert?

//                private fun eksporterSykefraværsstatistikkVirksomhet(årstallOgKvartal: ÅrstallOgKvartal) {
//                    val statistikk =
//                        sykefravarStatistikkVirksomhetRepository.hentSykefraværAlleVirksomheter(årstallOgKvartal inkludertTidligere 3)
//                            .groupBy({ it.orgnr }, { it })
//
//                    eksporterSykefraværsstatistikkPerKategori(
//                        eksportkvartal = årstallOgKvartal,
//                        sykefraværGruppertEtterKode = statistikk,
//                        statistikkategori = Statistikkategori.VIRKSOMHET,
//                        kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_VIRKSOMHET_V1
//                    )
//                }
//                private fun eksporterSykefraværsstatistikkVirksomhetGradert(årstallOgKvartal: ÅrstallOgKvartal) {
//                    sykefravarStatistikkVirksomhetGraderingRepository.hentSykefraværAlleVirksomheterGradert(
//                        årstallOgKvartal inkludertTidligere 3
//                    ).groupBy({ it.orgnr }, { it }).let {
//                        eksporterSykefraværsstatistikkPerKategori(
//                            eksportkvartal = årstallOgKvartal,
//                            sykefraværGruppertEtterKode = it,
//                            statistikkategori = Statistikkategori.VIRKSOMHET_GRADERT,
//                            kafkaTopic = KafkaTopic.SYKEFRAVARSSTATISTIKK_VIRKSOMHET_GRADERT_V1
//                        )
//                    }
//                }
        // TODO: Send resultat på kafka som DTO
    }
}
