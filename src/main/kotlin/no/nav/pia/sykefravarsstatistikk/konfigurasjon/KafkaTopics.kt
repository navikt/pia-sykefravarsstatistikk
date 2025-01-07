package no.nav.pia.sykefravarsstatistikk.konfigurasjon

import no.nav.pia.sykefravarsstatistikk.konfigurasjon.KafkaConfig.Companion.CLIENT_ID

enum class KafkaTopics(
    val navn: String,
    private val prefix: String = "pia",
) {
    KVARTALSVIS_SYKEFRAVARSSTATISTIKK_ØVRIGE_KATEGORIER("kvartalsvis-sykefravarsstatistikk-ovrige-kategorier-v1"),
    KVARTALSVIS_SYKEFRAVARSSTATISTIKK_VIRKSOMHET("kvartalsvis-sykefravarsstatistikk-virksomhet-v1"),
    ;

    val konsumentGruppe
        get() = "${navn}_$CLIENT_ID"

    val navnMedNamespace
        get() = "$prefix.$navn"
}
