package no.nav.pia.sykefravarsstatistikk.konfigurasjon

enum class Topic(
    val navn: String,
    val konsumentGruppe: String,
) {
    KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER(
        navn = "pia.kvartalsvis-sykefravarsstatistikk-ovrige-kategorier-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-ovrige-kategorier-consumer",
    ),
    KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET(
        navn = "pia.kvartalsvis-sykefravarsstatistikk-virksomhet-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-virksomhet-consumer",
    ),
    KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET_METADATA(
        navn = "pia.kvartalsvis-sykefravarsstatistikk-virksomhet-metadata-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-virksomhet-metadata-consumer",
    ),
    KVARTALSVIS_SYKEFRAVARSSTATISTIKK_PUBLISERINGSDATO(
        navn = "pia.kvartalsvis-sykefravarsstatistikk-publiseringsdato-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-publiseringsdato-consumer",
    ),
    STATISTIKK_EKSPORT_LAND(
        navn = "pia.sykefravarsstatistikk-land-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-land-produsent",
    ),
    STATISTIKK_EKSPORT_SEKTOR(
        navn = "pia.sykefravarsstatistikk-sektor-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-sektor-produsent",
    ),
    STATISTIKK_EKSPORT_NÆRING(
        navn = "pia.sykefravarsstatistikk-naring-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-naring-produsent",
    ),
    STATISTIKK_EKSPORT_BRANSJE(
        navn = "pia.sykefravarsstatistikk-bransje-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-bransje-produsent",
    ),
    STATISTIKK_EKSPORT_NÆRINGSKODE(
        navn = "pia.sykefravarsstatistikk-naringskode-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-naringskode-produsent",
    ),
    STATISTIKK_EKSPORT_VIRKSOMHET(
        navn = "pia.sykefravarsstatistikk-virksomhet-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-virksomhet-produsent",
    ),
    STATISTIKK_EKSPORT_VIRKSOMHET_GRADERT(
        navn = "pia.sykefravarsstatistikk-virksomhet-gradert-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-virksomhet-gradert-produsent",
    ),
    STATISTIKK_EKSPORT_METADATA_VIRKSOMHET(
        navn = "pia.sykefravarsstatistikk-metadata-virksomhet-v1",
        konsumentGruppe = "pia-sykefravarsstatistikk-metadata-virksomhet-produsent",
    ),
}
