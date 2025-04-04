package no.nav.pia.sykefravarsstatistikk.helper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregInstitusjonellSektorkodeDto
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregInstitusjonellSektorkodeDto.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.domene.BrregNæringskodeDto
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilDomene
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet

class TestdataHelper {
    companion object {
        // Variabler som kan brukes hvor det ikke er behov for å starte TestContainers (unit tester)
        const val ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2 = "3403:1"
        const val ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3 =
            "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"

        val overordnetEnhetMedEnkelrettighetBransjeBarnehage =
            OverordnetEnhet(
                orgnr = "100000001",
                navn = "Overordnet Enhet Med Enkelrettighet Bransje Barnehage",
                antallAnsatte = 50,
                næringskode = BrregNæringskodeDto(
                    kode = "88.911",
                    beskrivelse = "Barnehager",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "2100",
                    beskrivelse = "Private aksjeselskaper mv.",
                ).tilDomene(),
            )

        val underenhetMedEnkelrettighetBransjeBarnehage =
            Underenhet.Næringsdrivende(
                orgnr = "100000002",
                navn = "Underenhet Med Enkelrettighet Bransje Barnehage",
                antallAnsatte = 50,
                næringskode = BrregNæringskodeDto(
                    kode = "88.911",
                    beskrivelse = "Barnehager",
                ).tilDomene(),
                overordnetEnhetOrgnr = overordnetEnhetMedEnkelrettighetBransjeBarnehage.orgnr,
            )

        val overordnetEnhetMedTilhørighetBransjeSykehus =
            OverordnetEnhet(
                orgnr = "100000005",
                navn = "Overordnet Enhet Med Tilhørighet Bransje Sykehus",
                antallAnsatte = 100,
                næringskode = BrregNæringskodeDto(
                    kode = "86.101",
                    beskrivelse = "Alminnelige somatiske sykehus",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "6100",
                    beskrivelse = "Statsforvaltningen",
                ).tilDomene(),
            )

        val underenhetMedEnkelrettighetBransjeSykehus =
            Underenhet.Næringsdrivende(
                orgnr = "100000006",
                navn = "Underenhet Med Enkelrettighet Bransje Sykehus",
                antallAnsatte = 100,
                næringskode = BrregNæringskodeDto(
                    kode = "86.101",
                    beskrivelse = "Alminnelige somatiske sykehus",
                ).tilDomene(),
                overordnetEnhetOrgnr = overordnetEnhetMedTilhørighetBransjeSykehus.orgnr,
            )

        val overordnetEnhetMedTilhørighetBransjeBygg =
            OverordnetEnhet(
                orgnr = "100000003",
                navn = "Overordnet Enhet Med Tilhørighet Bransje Bygg",
                antallAnsatte = 150,
                næringskode = BrregNæringskodeDto(
                    kode = "41.200",
                    beskrivelse = "Oppføring av bygninger",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "2100",
                    beskrivelse = "Private aksjeselskaper mv.",
                ).tilDomene(),
            )

        val underenhetMedTilhørighetBransjeBygg =
            Underenhet.Næringsdrivende(
                orgnr = "100000004",
                navn = "Underenhet Med Tilhørighet Bransje Bygg",
                antallAnsatte = 150,
                næringskode = BrregNæringskodeDto(
                    kode = "41.200",
                    beskrivelse = "Oppføring av bygninger",
                ).tilDomene(),
                overordnetEnhetOrgnr = overordnetEnhetMedTilhørighetBransjeBygg.orgnr,
            )

        val overordnetEnhetMedEnkelrettighetUtenBransje =
            OverordnetEnhet(
                orgnr = "100000011",
                navn = "Overordnet Enhet Med Enkelrettighet Uten Bransje",
                antallAnsatte = 200,
                næringskode = BrregNæringskodeDto(
                    kode = "02.100",
                    beskrivelse = "Skogskjøtsel og andre skogbruksaktiviteter",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "8200",
                    beskrivelse = "Personlig næringsdrivende",
                ).tilDomene(),
            )

        val underenhetMedEnkelrettighetUtenBransje2 = Underenhet.Næringsdrivende(
            orgnr = "100000012",
            navn = "Underenhet Med Enkelrettighet Uten Bransje 2",
            antallAnsatte = 200,
            næringskode = BrregNæringskodeDto(
                kode = "02.100",
                beskrivelse = "Skogskjøtsel og andre skogbruksaktiviteter",
            ).tilDomene(),
            overordnetEnhetOrgnr = overordnetEnhetMedEnkelrettighetUtenBransje.orgnr,
        )

        val overordnetEnhetMedTilhørighetUtenBransje2 =
            OverordnetEnhet(
                orgnr = "100000009",
                navn = "Overordnet Enhet Med Tilhørighet Uten Bransje 2",
                antallAnsatte = 250,
                næringskode = BrregNæringskodeDto(
                    kode = "03.211",
                    beskrivelse = "Produksjon av matfisk og skalldyr i hav- og kystbasert fiskeoppdrett",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "2100",
                    beskrivelse = "Private aksjeselskaper mv.",
                ).tilDomene(),
            )

        val underenhetMedEnkelrettighetUtenBransje = Underenhet.Næringsdrivende(
            orgnr = "100000010",
            navn = "Underenhet Med Enkelrettighet Uten Bransje",
            antallAnsatte = 250,
            næringskode = BrregNæringskodeDto(
                kode = "03.211",
                beskrivelse = "Produksjon av matfisk og skalldyr i hav- og kystbasert fiskeoppdrett",
            ).tilDomene(),
            overordnetEnhetOrgnr = overordnetEnhetMedTilhørighetUtenBransje2.orgnr,
        )

        val overordnetEnhetMedTilhørighetUtenBransje =
            OverordnetEnhet(
                orgnr = "100000007",
                navn = "Overordnet Enhet Med Tilhørighet Uten Bransje",
                næringskode = BrregNæringskodeDto(
                    kode = "68.209",
                    beskrivelse = "Utleie av egen eller leid fast eiendom ellers",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "2100",
                    beskrivelse = "Private aksjeselskaper mv.",
                ).tilDomene(),
                antallAnsatte = 300,
            )

        val underenhetMedTilhørighetUtenBransje = Underenhet.Næringsdrivende(
            orgnr = "100000008",
            navn = "Underenhet Med Tilhørighet Uten Bransje",
            antallAnsatte = 300,
            næringskode = BrregNæringskodeDto(
                kode = "68.209",
                beskrivelse = "Utleie av egen eller leid fast eiendom ellers",
            ).tilDomene(),
            overordnetEnhetOrgnr = overordnetEnhetMedTilhørighetUtenBransje.orgnr,
        )

        val overordnetEnhetUtenStatistikk =
            OverordnetEnhet(
                orgnr = "100000013",
                navn = "Overordnet Enhet Uten Statistikk",
                antallAnsatte = 350,
                næringskode = BrregNæringskodeDto(
                    kode = "42.110",
                    beskrivelse = "Bygging av veier og motorveier",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "2100",
                    beskrivelse = "Private aksjeselskaper mv.",
                ).tilDomene(),
            )

        val enUnderenhetUtenStatistikk = Underenhet.Næringsdrivende(
            orgnr = "100000014",
            navn = "Underenhet Uten Statistikk",
            antallAnsatte = 350,
            næringskode = BrregNæringskodeDto(
                kode = "42.110",
                beskrivelse = "Bygging av veier og motorveier",
            ).tilDomene(),
            overordnetEnhetOrgnr = overordnetEnhetUtenStatistikk.orgnr,
        )

        val overordnetEnhetUtenTilgang =
            OverordnetEnhet(
                orgnr = "100000015",
                navn = "Overordnet Enhet Uten Tilgang",
                antallAnsatte = 400,
                næringskode = BrregNæringskodeDto(
                    kode = "09.109",
                    beskrivelse = "Andre tjenester tilknyttet utvinning av råolje og naturgass",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "1120",
                    beskrivelse = "Statlig eide aksjeselskaper mv.",
                ).tilDomene(),
            )

        val underenhetUtenTilgang = Underenhet.Næringsdrivende(
            orgnr = "100000016",
            navn = "Underenhet Uten Tilgang",
            antallAnsatte = 400,
            næringskode = BrregNæringskodeDto(
                kode = "09.109",
                beskrivelse = "Andre tjenester tilknyttet utvinning av råolje og naturgass",
            ).tilDomene(),
            overordnetEnhetOrgnr = overordnetEnhetUtenTilgang.orgnr,
        )
        val overordnetSykehjemUtenTilgang =
            OverordnetEnhet(
                orgnr = "100000017",
                navn = "Overordnet Enhet Med Tilhørighet Bransje Sykehjem",
                antallAnsatte = 400,
                næringskode = BrregNæringskodeDto(
                    kode = "87.102",
                    beskrivelse = "Somatisk sykehjem",
                ).tilDomene(),
                sektor = BrregInstitusjonellSektorkodeDto(
                    kode = "1120",
                    beskrivelse = "Statlig eide aksjeselskaper mv.",
                ).tilDomene(),
            )

        val underenhetSykehjemMedTilgang = Underenhet.Næringsdrivende(
            orgnr = "100000018",
            navn = "Underenhet Med Enkelrettighet Bransje Sykehjem",
            antallAnsatte = 400,
            næringskode = BrregNæringskodeDto(
                kode = "87.102",
                beskrivelse = "Somatisk sykehjem",
            ).tilDomene(),
            overordnetEnhetOrgnr = overordnetEnhetUtenTilgang.orgnr,
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
