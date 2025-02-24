package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkBransje
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkLand
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkSektor
import no.nav.pia.sykefravarsstatistikk.domene.SykefraværsstatistikkVirksomhet

@Serializable
data class KvartalsvisSykefraværshistorikkDto(
    val type: String,
    val label: String,
    val kvartalsvisSykefraværsprosent: List<KvartalsvisSykefraværsprosentDto>,
) {
    companion object {
        const val NAVNPÅVIRKSOMHET = "SPISS SJOKKERT TIGER AS"

        fun List<SykefraværsstatistikkVirksomhet>.tilVirksomhetDto() =
            KvartalsvisSykefraværshistorikkDto(
                type = "VIRKSOMHET",
                label = NAVNPÅVIRKSOMHET,
                kvartalsvisSykefraværsprosent = this.map { virksomhet ->
                    KvartalsvisSykefraværsprosentDto(
                        tapteDagsverk = virksomhet.tapteDagsverk,
                        muligeDagsverk = virksomhet.muligeDagsverk,
                        prosent = virksomhet.prosent,
                        erMaskert = false,
                        årstall = virksomhet.årstall,
                        kvartal = virksomhet.kvartal,
                    )
                },
            )

        fun List<SykefraværsstatistikkSektor>.tilSektorDto(sektor: String) =
            KvartalsvisSykefraværshistorikkDto(
                type = "SEKTOR",
                label = sektor,
                kvartalsvisSykefraværsprosent = this.map { bransje ->
                    KvartalsvisSykefraværsprosentDto(
                        tapteDagsverk = bransje.tapteDagsverk,
                        muligeDagsverk = bransje.muligeDagsverk,
                        prosent = bransje.prosent,
                        erMaskert = false,
                        årstall = bransje.årstall,
                        kvartal = bransje.kvartal,
                    )
                },
            )

        fun List<SykefraværsstatistikkLand>.tilLandDto() =
            KvartalsvisSykefraværshistorikkDto(
                type = "LAND",
                label = this.first().land,
                kvartalsvisSykefraværsprosent = this.map { land ->
                    KvartalsvisSykefraværsprosentDto(
                        tapteDagsverk = land.tapteDagsverk,
                        muligeDagsverk = land.muligeDagsverk,
                        prosent = land.prosent,
                        erMaskert = false,
                        årstall = land.årstall,
                        kvartal = land.kvartal,
                    )
                },
            )

        fun List<SykefraværsstatistikkBransje>.tilBransjeDto(bransje: String) =
            KvartalsvisSykefraværshistorikkDto(
                type = "BRANSJE",
                label = bransje,
                kvartalsvisSykefraværsprosent = this.map { bransjestatistikk ->
                    KvartalsvisSykefraværsprosentDto(
                        tapteDagsverk = bransjestatistikk.tapteDagsverk,
                        muligeDagsverk = bransjestatistikk.muligeDagsverk,
                        prosent = bransjestatistikk.prosent,
                        erMaskert = false,
                        årstall = bransjestatistikk.årstall,
                        kvartal = bransjestatistikk.kvartal,
                    )
                },
            )
    }
}

@Serializable
data class KvartalsvisSykefraværsprosentDto(
    val tapteDagsverk: Double,
    val muligeDagsverk: Double,
    val prosent: Double,
    val erMaskert: Boolean,
    val årstall: Int,
    val kvartal: Int,
)
