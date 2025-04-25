package no.nav.pia.sykefravarsstatistikk.api.auth

class AltinnTilgangerTestCase {
    companion object {

        // Respons fra arbeidsgiver-altinn-tilganger i dev-miljø: https://arbeidsgiver-altinn-tilganger.intern.dev.nav.no/swagger-ui#
        val jsonResponseFraAltinnTilgangerIDevMiljø: String = """
            {
              "isError": false,
              "hierarki": [
                {
                  "orgnr": "310529915",
                  "altinn3Tilganger": [
                    "nav_forebygge-og-redusere-sykefravar_samarbeid",
                    "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk",
                    "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger",
                    "nav_sosialtjenester_digisos-avtale"
                  ],
                  "altinn2Tilganger": [
                    "5934:1",
                    "3403:1",
                    "5810:1",
                    "5867:1",
                    "2896:87",
                    "5516:3",
                    "5516:1",
                    "5516:4",
                    "5441:1",
                    "5516:6",
                    "5384:1",
                    "5516:5",
                    "5332:1",
                    "5516:2",
                    "4826:1",
                    "4936:1",
                    "5278:1",
                    "5078:1",
                    "5902:1"
                  ],
                  "underenheter": [
                    {
                      "orgnr": "311874411",
                      "altinn3Tilganger": [
                        "nav_forebygge-og-redusere-sykefravar_samarbeid",
                        "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk",
                        "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger",
                        "nav_sosialtjenester_digisos-avtale"
                      ],
                      "altinn2Tilganger": [
                        "5934:1",
                        "3403:1",
                        "5810:1",
                        "5867:1",
                        "2896:87",
                        "5516:3",
                        "5516:1",
                        "5516:4",
                        "5441:1",
                        "5516:6",
                        "5384:1",
                        "5516:5",
                        "5332:1",
                        "5516:2",
                        "4826:1",
                        "4936:1",
                        "5278:1",
                        "5078:1",
                        "5902:1"
                      ],
                      "underenheter": [],
                      "navn": "SPISS SJOKKERT TIGER AS",
                      "organisasjonsform": "BEDR"
                    }
                  ],
                  "navn": "SPISS SJOKKERT TIGER AS",
                  "organisasjonsform": "AS"
                },
                {
                  "orgnr": "313068420",
                  "altinn3Tilganger": [
                    "nav_forebygge-og-redusere-sykefravar_samarbeid"
                  ],
                  "altinn2Tilganger": [
                    "5934:1",
                    "4826:1",
                    "4936:1",
                    "5078:1",
                    "5902:1"
                  ],
                  "underenheter": [
                    {
                      "orgnr": "315829062",
                      "altinn3Tilganger": [
                        "nav_forebygge-og-redusere-sykefravar_samarbeid"
                      ],
                      "altinn2Tilganger": [
                        "5934:1",
                        "4826:1",
                        "4936:1",
                        "5078:1",
                        "5902:1"
                      ],
                      "underenheter": [],
                      "navn": "TILLITSFULL PEN TIGER AS",
                      "organisasjonsform": "BEDR"
                    }
                  ],
                  "navn": "TILLITSFULL PEN TIGER AS",
                  "organisasjonsform": "AS"
                },
                {
                  "orgnr": "310807176",
                  "altinn3Tilganger": [
                    "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"
                  ],
                  "altinn2Tilganger": [
                    "3403:1"
                  ],
                  "underenheter": [],
                  "navn": "UVITENDE SNILL TIGER AS",
                  "organisasjonsform": "AS"
                },
                {
                  "orgnr": "313901637",
                  "altinn3Tilganger": [
                    "nav_forebygge-og-redusere-sykefravar_samarbeid"
                  ],
                  "altinn2Tilganger": [
                    "5934:1"
                  ],
                  "underenheter": [
                    {
                      "orgnr": "311284568",
                      "altinn3Tilganger": [
                        "nav_forebygge-og-redusere-sykefravar_samarbeid"
                      ],
                      "altinn2Tilganger": [
                        "5934:1"
                      ],
                      "underenheter": [],
                      "navn": "ÆRLIG EKSPLOSIV TIGER AS",
                      "organisasjonsform": "BEDR"
                    }
                  ],
                  "navn": "ÆRLIG EKSPLOSIV TIGER AS",
                  "organisasjonsform": "AS"
                }
              ],
              "orgNrTilTilganger": {
                "310529915": [
                  "5934:1",
                  "3403:1",
                  "5810:1",
                  "5867:1",
                  "2896:87",
                  "5516:3",
                  "5516:1",
                  "5516:4",
                  "5441:1",
                  "5516:6",
                  "5384:1",
                  "5516:5",
                  "5332:1",
                  "5516:2",
                  "4826:1",
                  "4936:1",
                  "5278:1",
                  "5078:1",
                  "5902:1",
                  "nav_forebygge-og-redusere-sykefravar_samarbeid",
                  "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk",
                  "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger",
                  "nav_sosialtjenester_digisos-avtale"
                ],
                "310807176": [
                  "3403:1",
                  "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk"
                ],
                "311284568": [
                  "5934:1",
                  "nav_forebygge-og-redusere-sykefravar_samarbeid"
                ],
                "311874411": [
                  "5934:1",
                  "3403:1",
                  "5810:1",
                  "5867:1",
                  "2896:87",
                  "5516:3",
                  "5516:1",
                  "5516:4",
                  "5441:1",
                  "5516:6",
                  "5384:1",
                  "5516:5",
                  "5332:1",
                  "5516:2",
                  "4826:1",
                  "4936:1",
                  "5278:1",
                  "5078:1",
                  "5902:1",
                  "nav_forebygge-og-redusere-sykefravar_samarbeid",
                  "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk",
                  "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger",
                  "nav_sosialtjenester_digisos-avtale"
                ],
                "313068420": [
                  "5934:1",
                  "4826:1",
                  "4936:1",
                  "5078:1",
                  "5902:1",
                  "nav_forebygge-og-redusere-sykefravar_samarbeid"
                ],
                "313901637": [
                  "5934:1",
                  "nav_forebygge-og-redusere-sykefravar_samarbeid"
                ],
                "315829062": [
                  "5934:1",
                  "4826:1",
                  "4936:1",
                  "5078:1",
                  "5902:1",
                  "nav_forebygge-og-redusere-sykefravar_samarbeid"
                ]
              },
              "tilgangTilOrgNr": {
                "5934:1": [
                  "310529915",
                  "311874411",
                  "313068420",
                  "315829062",
                  "313901637",
                  "311284568"
                ],
                "3403:1": [
                  "310529915",
                  "311874411",
                  "310807176"
                ],
                "5810:1": [
                  "310529915",
                  "311874411"
                ],
                "5867:1": [
                  "310529915",
                  "311874411"
                ],
                "2896:87": [
                  "310529915",
                  "311874411"
                ],
                "5516:3": [
                  "310529915",
                  "311874411"
                ],
                "5516:1": [
                  "310529915",
                  "311874411"
                ],
                "5516:4": [
                  "310529915",
                  "311874411"
                ],
                "5441:1": [
                  "310529915",
                  "311874411"
                ],
                "5516:6": [
                  "310529915",
                  "311874411"
                ],
                "5384:1": [
                  "310529915",
                  "311874411"
                ],
                "5516:5": [
                  "310529915",
                  "311874411"
                ],
                "5332:1": [
                  "310529915",
                  "311874411"
                ],
                "5516:2": [
                  "310529915",
                  "311874411"
                ],
                "4826:1": [
                  "310529915",
                  "311874411",
                  "313068420",
                  "315829062"
                ],
                "4936:1": [
                  "310529915",
                  "311874411",
                  "313068420",
                  "315829062"
                ],
                "5278:1": [
                  "310529915",
                  "311874411"
                ],
                "5078:1": [
                  "310529915",
                  "311874411",
                  "313068420",
                  "315829062"
                ],
                "5902:1": [
                  "310529915",
                  "311874411",
                  "313068420",
                  "315829062"
                ],
                "nav_forebygge-og-redusere-sykefravar_samarbeid": [
                  "310529915",
                  "311874411",
                  "313068420",
                  "315829062",
                  "313901637",
                  "311284568"
                ],
                "nav_forebygge-og-redusere-sykefravar_sykefravarsstatistikk": [
                  "310529915",
                  "311874411",
                  "310807176"
                ],
                "nav_permittering-og-nedbemmaning_innsyn-i-alle-innsendte-meldinger": [
                  "310529915",
                  "311874411"
                ],
                "nav_sosialtjenester_digisos-avtale": [
                  "310529915",
                  "311874411"
                ]
              }
            }
        """.trimIndent()
    }
}
