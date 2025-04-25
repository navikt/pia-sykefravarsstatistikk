package no.nav.pia.sykefravarsstatistikk.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregEnhetDto
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregInstitusjonellSektorkodeDto
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregInstitusjonellSektorkodeDto.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregUnderenhetDto
import no.nav.pia.sykefravarsstatistikk.domene.BrregNæringskodeDto
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.helper.BrregInstitusjonellSektorkoder.Companion.PERSONLIG_NÆRINGSDRIVENDE
import no.nav.pia.sykefravarsstatistikk.helper.BrregInstitusjonellSektorkoder.Companion.PRIVAT_AKS
import no.nav.pia.sykefravarsstatistikk.helper.BrregInstitusjonellSektorkoder.Companion.STATLIG_EIDE_AKS
import no.nav.pia.sykefravarsstatistikk.helper.BrregInstitusjonellSektorkoder.Companion.STATSFORVALTNINGEN
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.ALMN_SOMATISKE_SYKEHUS
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.BYGGING_AV_VEIER
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.NÆRINGSKODE_BARNEHAGER
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.OPPFØRING_AV_BYGNINGER
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.PRODUKSJON_AV_MATFISK
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.SKOGSKJØTSEL
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.SOMATISK_SYKEHJEM
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.UTLEIE_AV_EIENDOM
import no.nav.pia.sykefravarsstatistikk.helper.BrregNæringskoder.Companion.UTVINNING_AV_RÅOLJE_OG_GASS
import no.nav.pia.sykefravarsstatistikk.persistering.BigDecimalSerializer
import java.math.BigDecimal

class TestdataHelper {
    companion object {
        // Variabler som kan brukes hvor det ikke er behov for å starte TestContainers (unit tester)
        const val ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3 =
            "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"

        fun BrregUnderenhetDto.somNæringsdrivende() = Underenhet.Næringsdrivende(
            orgnr = this.organisasjonsnummer,
            navn = this.navn,
            antallAnsatte = this.antallAnsatte?: 0,
            næringskode = this.naeringskode1!!.tilDomene(),
            overordnetEnhetOrgnr = this.overordnetEnhet,
        )

        fun BrregUnderenhetDto.somIkkeNæringsdrivende() = Underenhet.IkkeNæringsdrivende(
            orgnr = this.organisasjonsnummer,
            navn = this.navn,
            overordnetEnhetOrgnr = this.overordnetEnhet,
        )

        fun BrregEnhetDto.somOverordnetEnhet() = OverordnetEnhet(
            orgnr = this.organisasjonsnummer,
            navn = this.navn,
            antallAnsatte = this.antallAnsatte,
            næringskode = this.naeringskode1.tilDomene(),
            sektor = this.institusjonellSektorkode?.tilDomene(),
        )

        // Med bransje
        val overordnetEnhetIBransjeBarnehage =
            BrregEnhetDto(
                organisasjonsnummer = "100000001",
                navn = "Overordnet Enhet Med Enkelrettighet Bransje Barnehage",
                antallAnsatte = 50,
                naeringskode1 = NÆRINGSKODE_BARNEHAGER,
                overordnetEnhet = null,
                institusjonellSektorkode = PRIVAT_AKS,
            )

        val underenhetIBransjeBarnehage =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000002",
                navn = "Underenhet Med Enkelrettighet Bransje Barnehage",
                antallAnsatte = 50,
                naeringskode1 = NÆRINGSKODE_BARNEHAGER,
                overordnetEnhet = overordnetEnhetIBransjeBarnehage.organisasjonsnummer,
            )

        val overordnetEnhetIBransjeSykehus =
            BrregEnhetDto(
                organisasjonsnummer = "100000005",
                navn = "Overordnet Enhet Med Tilhørighet Bransje Sykehus",
                antallAnsatte = 100,
                naeringskode1 = ALMN_SOMATISKE_SYKEHUS,
                institusjonellSektorkode = STATSFORVALTNINGEN,
            )

        val underenhetIBransjeSykehus =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000006",
                navn = "Underenhet Med Enkelrettighet Bransje Sykehus",
                antallAnsatte = 100,
                naeringskode1 = ALMN_SOMATISKE_SYKEHUS,
                overordnetEnhet = overordnetEnhetIBransjeSykehus.organisasjonsnummer,
            )

        val overordnetEnhetIBransjeAnlegg =
            BrregEnhetDto(
                organisasjonsnummer = "100000013",
                navn = "Overordnet Enhet Uten Statistikk",
                antallAnsatte = 350,
                naeringskode1 = BYGGING_AV_VEIER,
                institusjonellSektorkode = PRIVAT_AKS,
            )

        val underenhetIBransjeAnlegg =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000014",
                navn = "Underenhet Uten Statistikk",
                antallAnsatte = 350,
                naeringskode1 = BYGGING_AV_VEIER,
                overordnetEnhet = overordnetEnhetIBransjeAnlegg.organisasjonsnummer,
            )

        val overordnetEnhetIBransjeSykehjem =
            BrregEnhetDto(
                organisasjonsnummer = "100000017",
                navn = "Overordnet Enhet Med Tilhørighet Bransje Sykehjem",
                antallAnsatte = 400,
                naeringskode1 = SOMATISK_SYKEHJEM,
                institusjonellSektorkode = STATLIG_EIDE_AKS,
            )

        val underenhetIBransjeSykehjem =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000018",
                navn = "Underenhet Med Enkelrettighet Bransje Sykehjem",
                antallAnsatte = 400,
                naeringskode1 = SOMATISK_SYKEHJEM,
                overordnetEnhet = overordnetEnhetIBransjeSykehjem.organisasjonsnummer,
            )

        val overordnetEnhetIBransjeByggUtenInstitusjonellSektorKode =
            BrregEnhetDto(
                organisasjonsnummer = "100000019",
                navn = "Overordnet Enhet Med Tilhørighet Bransje Bygg uten sektor kode",
                antallAnsatte = 150,
                naeringskode1 = OPPFØRING_AV_BYGNINGER,
                institusjonellSektorkode = null,
            )

        val underenhetIBransjeByggUtenInstitusjonellSektorKode =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000020",
                navn = "Underenhet Med Tilhørighet Bransje Bygg uten sektor kode",
                antallAnsatte = 150,
                naeringskode1 = OPPFØRING_AV_BYGNINGER,
                overordnetEnhet = overordnetEnhetIBransjeByggUtenInstitusjonellSektorKode.organisasjonsnummer,
            )

        // Med næring (dvs uten bransje)
        val overordnetEnhetINæringUtleieAvEiendom =
            BrregEnhetDto(
                organisasjonsnummer = "100000007",
                navn = "Overordnet Enhet Med Tilhørighet Uten Bransje",
                naeringskode1 = UTLEIE_AV_EIENDOM,
                overordnetEnhet = null,
                antallAnsatte = 300,
                institusjonellSektorkode = PRIVAT_AKS,
            )

        val underenhetINæringUtleieAvEiendom =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000008",
                navn = "Underenhet Med Tilhørighet Uten Bransje",
                antallAnsatte = 300,
                naeringskode1 = UTLEIE_AV_EIENDOM,
                overordnetEnhet = overordnetEnhetINæringUtleieAvEiendom.organisasjonsnummer,
            )

        val overordnetEnhetINæringProduksjonAvMatfisk =
            BrregEnhetDto(
                organisasjonsnummer = "100000009",
                navn = "Overordnet Enhet Med Tilhørighet Uten Bransje 2",
                antallAnsatte = 250,
                naeringskode1 = PRODUKSJON_AV_MATFISK,
                institusjonellSektorkode = PRIVAT_AKS,
            )

        val underenhetINæringProduksjonAvMatfisk =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000010",
                navn = "Underenhet Med Enkelrettighet Uten Bransje",
                antallAnsatte = 250,
                naeringskode1 = PRODUKSJON_AV_MATFISK,
                overordnetEnhet = overordnetEnhetINæringProduksjonAvMatfisk.organisasjonsnummer,
            )

        val overordnetEnhetINæringSkogskjøtsel =
            BrregEnhetDto(
                organisasjonsnummer = "100000011",
                navn = "Overordnet Enhet Med Enkelrettighet Uten Bransje",
                antallAnsatte = 200,
                naeringskode1 = SKOGSKJØTSEL,
                institusjonellSektorkode = PERSONLIG_NÆRINGSDRIVENDE,
            )

        val underenhetINæringSkogskjøtsel =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000012",
                navn = "Underenhet Med Enkelrettighet Uten Bransje 2",
                antallAnsatte = 200,
                naeringskode1 = SKOGSKJØTSEL,
                overordnetEnhet = overordnetEnhetINæringSkogskjøtsel.organisasjonsnummer,
            )

        val overordnetEnhetINæringUtvinningAvRåoljeOgGass =
            BrregEnhetDto(
                organisasjonsnummer = "100000015",
                navn = "Overordnet Enhet Uten Tilgang",
                antallAnsatte = 400,
                naeringskode1 = UTVINNING_AV_RÅOLJE_OG_GASS,
                institusjonellSektorkode = STATLIG_EIDE_AKS,
            )

        val underenhetINæringUtvinningAvRåoljeOgGass =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000016",
                navn = "Underenhet Uten Tilgang",
                antallAnsatte = 400,
                naeringskode1 = UTVINNING_AV_RÅOLJE_OG_GASS,
                overordnetEnhet = overordnetEnhetINæringUtvinningAvRåoljeOgGass.organisasjonsnummer,
            )
        // Edge case
        val overordnetEnhetMedUnderenhetUtenNæringskode =
            BrregEnhetDto(
                organisasjonsnummer = "100000021",
                navn = "Overordnet Enhet Uten Næringskode",
                antallAnsatte = 150,
                naeringskode1 = UTVINNING_AV_RÅOLJE_OG_GASS, // usikker om det er mulig å IKKE ha næringskode her
                institusjonellSektorkode = PRIVAT_AKS,
            )

        val underenhetUtenNæringskode =
            BrregUnderenhetDto(
                organisasjonsnummer = "100000022",
                navn = "Underenhet Uten Næringskode",
                antallAnsatte = 150,
                naeringskode1 = null,
                overordnetEnhet = overordnetEnhetMedUnderenhetUtenNæringskode.organisasjonsnummer,
            )

        val virksomheterMedBransje: List<Pair<BrregEnhetDto, BrregUnderenhetDto>> =
            listOf(
                Pair(overordnetEnhetIBransjeBarnehage, underenhetIBransjeBarnehage),
                Pair(overordnetEnhetIBransjeSykehus, underenhetIBransjeSykehus),
                Pair(overordnetEnhetIBransjeAnlegg, underenhetIBransjeAnlegg),
                Pair(overordnetEnhetIBransjeSykehjem, underenhetIBransjeSykehjem),
            )

        val virksomheterMedNæring: List<Pair<BrregEnhetDto, BrregUnderenhetDto>> =
            listOf(
                Pair(overordnetEnhetINæringUtleieAvEiendom, underenhetINæringUtleieAvEiendom),
                Pair(overordnetEnhetINæringProduksjonAvMatfisk, underenhetINæringProduksjonAvMatfisk),
                Pair(overordnetEnhetINæringSkogskjøtsel, underenhetINæringSkogskjøtsel),
                Pair(overordnetEnhetINæringUtvinningAvRåoljeOgGass, underenhetINæringUtvinningAvRåoljeOgGass),
            )

        val virksomheterMedEdgeCase: List<Pair<BrregEnhetDto, BrregUnderenhetDto>> =
            listOf(
                Pair(
                    overordnetEnhetIBransjeByggUtenInstitusjonellSektorKode,
                    underenhetIBransjeByggUtenInstitusjonellSektorKode,
                ),
                Pair(
                    overordnetEnhetMedUnderenhetUtenNæringskode,
                    underenhetUtenNæringskode,
                ),
            )


        @OptIn(ExperimentalSerializationApi::class)
        inline fun <reified T> prettyPrint(statistikk: T) {
            val prettyJson = Json {
                prettyPrint = true
                prettyPrintIndent = " "
            }
            println("[DEBUG][Test] Statistikk: \n ${prettyJson.encodeToString(value = statistikk)}")
        }
    }
}

internal class BrregInstitusjonellSektorkoder {
    companion object {
        val PRIVAT_AKS = BrregInstitusjonellSektorkodeDto(
            kode = "2100",
            beskrivelse = "Private aksjeselskaper mv.",
        )
        val STATLIG_EIDE_AKS = BrregInstitusjonellSektorkodeDto(
            kode = "1120",
            beskrivelse = "Statlig eide aksjeselskaper mv.",
        )
        val STATSFORVALTNINGEN = BrregInstitusjonellSektorkodeDto(
            kode = "6100",
            beskrivelse = "Statsforvaltningen",
        )
        val PERSONLIG_NÆRINGSDRIVENDE = BrregInstitusjonellSektorkodeDto(
            kode = "8200",
            beskrivelse = "Personlig næringsdrivende",
        )
    }
}

internal class BrregNæringskoder {
    companion object {
        val SKOGSKJØTSEL = BrregNæringskodeDto(
            kode = "02.100",
            beskrivelse = "Skogskjøtsel og andre skogbruksaktiviteter",
        )
        val PRODUKSJON_AV_MATFISK = BrregNæringskodeDto(
            kode = "03.211",
            beskrivelse = "Produksjon av matfisk og skalldyr i hav- og kystbasert fiskeoppdrett",
        )
        val UTVINNING_AV_RÅOLJE_OG_GASS = BrregNæringskodeDto(
            kode = "09.109",
            beskrivelse = "Andre tjenester tilknyttet utvinning av råolje og naturgass",
        )
        val BYGGING_AV_VEIER = BrregNæringskodeDto(
            kode = "42.110",
            beskrivelse = "Bygging av veier og motorveier",
        )
        val UTLEIE_AV_EIENDOM = BrregNæringskodeDto(
            kode = "68.209",
            beskrivelse = "Utleie av egen eller leid fast eiendom ellers",
        )
        val ALMN_SOMATISKE_SYKEHUS = BrregNæringskodeDto(
            kode = "86.101",
            beskrivelse = "Alminnelige somatiske sykehus",
        )
        val SOMATISK_SYKEHJEM = BrregNæringskodeDto(
            kode = "87.102",
            beskrivelse = "Somatisk sykehjem",
        )
        val NÆRINGSKODE_BARNEHAGER = BrregNæringskodeDto(
            kode = "88.911",
            beskrivelse = "Barnehager",
        )
        val OPPFØRING_AV_BYGNINGER = BrregNæringskodeDto(
            kode = "41.200",
            beskrivelse = "Oppføring av bygninger",
        )
    }
}

@Serializable
data class KvartalsvisSykefraværshistorikkTestDto(
    val type: String,
    val label: String,
    val kvartalsvisSykefraværsprosent: List<KvartalsvisSykefraværsprosentTestDto>,
)

@Serializable
data class KvartalsvisSykefraværsprosentTestDto(
    @Serializable(with = BigDecimalSerializer::class)
    val tapteDagsverk: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val muligeDagsverk: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val prosent: BigDecimal?,
    val erMaskert: Boolean,
    val årstall: Int,
    val kvartal: Int,
)
