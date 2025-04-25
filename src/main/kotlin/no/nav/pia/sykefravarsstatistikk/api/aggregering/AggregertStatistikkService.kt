package no.nav.pia.sykefravarsstatistikk.api.aggregering

import arrow.core.Either
import arrow.core.right
import ia.felles.definisjoner.bransjer.Bransje
import no.nav.pia.sykefravarsstatistikk.api.auth.VerifiserteTilganger
import no.nav.pia.sykefravarsstatistikk.api.dto.AggregertStatistikkResponseDto
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.exceptions.Feil
import no.nav.pia.sykefravarsstatistikk.persistering.ImporttidspunktRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkGraderingRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkMedVarighetRepository
import no.nav.pia.sykefravarsstatistikk.persistering.SykefraværsstatistikkRepository

class AggregertStatistikkService(
    private val importtidspunktRepository: ImporttidspunktRepository,
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
    private val sykefraværsstatistikkMedVarighetRepository: SykefraværsstatistikkMedVarighetRepository,
    private val sykefraværsstatistikkGraderingRepository: SykefraværsstatistikkGraderingRepository,
) {
    fun hentAggregertStatistikk(
        underenhet: Underenhet.Næringsdrivende,
        tilganger: VerifiserteTilganger,
    ): Either<Feil, AggregertStatistikkResponseDto> {
        val virksomhet = underenhet
        val gjeldendePeriode = ÅrstallOgKvartal(årstall = 2024, kvartal = 4)

        val aggregeringskategorier = buildList {
            add(Aggregeringskategorier.Land)
            if (tilganger.harEnkeltrettighetUnderenhet) {
                add(Aggregeringskategorier.Virksomhet(virksomhet))
            }

            val bransje = virksomhet.bransje()
            if (bransje == null) {
                add(Aggregeringskategorier.Næring(virksomhet.næringskode.næring))
            } else {
                add(Aggregeringskategorier.Bransje(bransje))
            }
        }

        val totalSykefravær = hentTotaltSykefraværAlleKategorier(
            kvartaler = gjeldendePeriode.sisteFemKvartaler(),
            kategorier = aggregeringskategorier,
        )
        val gradertSykefravær = hentGradertSykefraværAlleKategorier(aggregeringskategorier)
        val korttidSykefravær = hentKortidsfravær(aggregeringskategorier)
        val langtidsfravær = hentLangtidsfravær(aggregeringskategorier)

        return aggregerData(
            virksomhet = virksomhet,
            totalfravær = totalSykefravær,
            gradertFravær = gradertSykefravær,
            korttidsfravær = korttidSykefravær,
            langtidsfravær = langtidsfravær,
        ).right()
    }

    private fun hentGradertSykefraværAlleKategorier(aggregerbare: List<Aggregeringskategorier>): Sykefraværsdata {
        val data: Map<Aggregeringskategorier, List<UmaskertSykefraværUtenProsentForEttKvartal>> =
            aggregerbare.mapNotNull {
                when (it) {
                    Aggregeringskategorier.Land -> null
                    is Aggregeringskategorier.Næring -> it to sykefraværsstatistikkGraderingRepository.hentForNæring(it.næring)
                    is Aggregeringskategorier.Bransje -> it to sykefraværsstatistikkGraderingRepository.hentForBransje(
                        it.bransje,
                    )

                    is Aggregeringskategorier.Virksomhet -> it to sykefraværsstatistikkGraderingRepository.hentForVirksomhet(
                        it.virksomhet,
                    )
                }
            }.toMap()
        return Sykefraværsdata(sykefravær = data)
    }

    private fun aggregerData(
        virksomhet: Underenhet.Næringsdrivende,
        totalfravær: Sykefraværsdata,
        gradertFravær: Sykefraværsdata,
        korttidsfravær: Sykefraværsdata,
        langtidsfravær: Sykefraværsdata,
    ): AggregertStatistikkResponseDto {
        val sistePubliserteKvartal = importtidspunktRepository.hentNyesteImporterteKvartal()

        val kalkulatorTotal = Aggregeringskalkulator(totalfravær, sistePubliserteKvartal)
        val kalkulatorGradert = Aggregeringskalkulator(gradertFravær, sistePubliserteKvartal)
        val kalkulatorKorttid = Aggregeringskalkulator(korttidsfravær, sistePubliserteKvartal)
        val kalkulatorLangtid = Aggregeringskalkulator(langtidsfravær, sistePubliserteKvartal)
        val bransjeEllerNæring = finnBransjeEllerNæring(virksomhet)

        val prosentSisteFireKvartalerTotalt = arrayOf(
            kalkulatorTotal.fraværsprosentVirksomhet(virksomhet.navn),
            kalkulatorTotal.fraværsprosentBransjeEllerNæring(bransjeEllerNæring),
            kalkulatorTotal.fraværsprosentNorge(),
        ).mapNotNull { it.getOrNull() }

        val prosentSisteFireKvartalerGradert = arrayOf(
            kalkulatorGradert.fraværsprosentVirksomhet(virksomhet.navn),
            kalkulatorGradert.fraværsprosentBransjeEllerNæring(bransjeEllerNæring),
        ).mapNotNull { it.getOrNull() }

        val prosentSisteFireKvartalerKorttid = arrayOf(
            kalkulatorKorttid.fraværsprosentVirksomhet(virksomhet.navn),
            kalkulatorKorttid.fraværsprosentBransjeEllerNæring(bransjeEllerNæring),
        ).mapNotNull { it.getOrNull() }

        val prosentSisteFireKvartalerLangtid = arrayOf(
            kalkulatorLangtid.fraværsprosentVirksomhet(virksomhet.navn),
            kalkulatorLangtid.fraværsprosentBransjeEllerNæring(bransjeEllerNæring),
        ).mapNotNull { it.getOrNull() }

        val trendTotalt = arrayOf(
            kalkulatorTotal.trendBransjeEllerNæring(bransjeEllerNæring),
        ).mapNotNull { it.getOrNull() }

        val tapteDagsverkTotalt = arrayOf(
            kalkulatorTotal.tapteDagsverkVirksomhet(virksomhet.navn),
        ).mapNotNull { it.getOrNull() }

        val muligeDagsverkTotalt = arrayOf(
            kalkulatorTotal.muligeDagsverkVirksomhet(virksomhet.navn),
        ).mapNotNull { it.getOrNull() }

        return AggregertStatistikkResponseDto(
            prosentSisteFireKvartalerTotalt,
            prosentSisteFireKvartalerGradert,
            prosentSisteFireKvartalerKorttid,
            prosentSisteFireKvartalerLangtid,
            trendTotalt,
            tapteDagsverkTotalt,
            muligeDagsverkTotalt,
        )
    }

    fun finnBransjeEllerNæring(underenhet: Underenhet.Næringsdrivende): BransjeEllerNæring =
        Bransje.fra(underenhet.næringskode.femsifferIdentifikator)?.let {
            BransjeEllerNæring(it)
        } ?: BransjeEllerNæring(Næring(tosifferIdentifikator = underenhet.næringskode.næring.tosifferIdentifikator))

    fun hentTotaltSykefraværAlleKategorier(
        kvartaler: List<ÅrstallOgKvartal>,
        kategorier: List<Aggregeringskategorier>,
    ): Sykefraværsdata {
        val data: Map<Aggregeringskategorier, List<UmaskertSykefraværUtenProsentForEttKvartal>> =
            kategorier.associateWith {
                when (it) {
                    Aggregeringskategorier.Land -> sykefraværsstatistikkRepository.hentSykefraværsstatistikkLand()
                        .toUmaskertSykefraværUtenProsentForEttKvartal()
                        .filterPåKvartaler(kvartaler)

                    is Aggregeringskategorier.Næring -> sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(
                        næring = it.næring,
                    ).toUmaskertSykefraværUtenProsentForEttKvartal().filterPåKvartaler(kvartaler)

                    is Aggregeringskategorier.Virksomhet -> sykefraværsstatistikkRepository.hentSykefraværsstatistikkVirksomhet(
                        virksomhet = it.virksomhet,
                    ).toUmaskertSykefraværUtenProsentForEttKvartal().filterPåKvartaler(kvartaler)

                    is Aggregeringskategorier.Bransje -> sykefraværsstatistikkRepository.hentSykefraværsstatistikkBransje(
                        bransje = it.bransje,
                    ).toUmaskertSykefraværUtenProsentForEttKvartal().filterPåKvartaler(kvartaler)
                }
            }

        return Sykefraværsdata(data)
    }

    private fun hentLangtidsfravær(aggregeringskategorier: List<Aggregeringskategorier>): Sykefraværsdata {
        val data: Map<Aggregeringskategorier, List<UmaskertSykefraværUtenProsentForEttKvartal>> =
            aggregeringskategorier.mapNotNull {
                when (it) {
                    Aggregeringskategorier.Land -> null
                    is Aggregeringskategorier.Bransje -> it to sykefraværsstatistikkMedVarighetRepository.hentLangtidsfravær(
                        it.bransje,
                    )

                    is Aggregeringskategorier.Næring -> it to sykefraværsstatistikkMedVarighetRepository.hentLangtidsfravær(
                        it.næring,
                    )

                    is Aggregeringskategorier.Virksomhet -> it to sykefraværsstatistikkMedVarighetRepository.hentLangtidsfravær(
                        it.virksomhet,
                    )
                }
            }.toMap()

        return Sykefraværsdata(data)
    }

    private fun hentKortidsfravær(aggregeringskategorier: List<Aggregeringskategorier>): Sykefraværsdata {
        val data = aggregeringskategorier.mapNotNull {
            when (it) {
                Aggregeringskategorier.Land -> null
                is Aggregeringskategorier.Bransje -> it to sykefraværsstatistikkMedVarighetRepository.hentKorttidsfravær(
                    it.bransje,
                )

                is Aggregeringskategorier.Næring -> it to sykefraværsstatistikkMedVarighetRepository.hentKorttidsfravær(
                    it.næring,
                )

                is Aggregeringskategorier.Virksomhet -> it to sykefraværsstatistikkMedVarighetRepository.hentKorttidsfravær(
                    it.virksomhet,
                )
            }
        }.toMap()

        return Sykefraværsdata(data)
    }

    private fun List<Sykefraværsstatistikk>.toUmaskertSykefraværUtenProsentForEttKvartal(): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        this.map {
            UmaskertSykefraværUtenProsentForEttKvartal(it)
        }

    private fun List<UmaskertSykefraværUtenProsentForEttKvartal>.filterPåKvartaler(
        kvartaler: List<ÅrstallOgKvartal>,
    ): List<UmaskertSykefraværUtenProsentForEttKvartal> {
        this.filter {
            kvartaler.any { kvartal ->
                it.årstallOgKvartal == kvartal
            }
        }.let {
            return it
        }
    }
}
