package no.nav.pia.sykefravarsstatistikk.api.aggregering

import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal


class Sykefraværsdata(
    val sykefravær: Map<Aggregeringskategorier, List<UmaskertSykefraværUtenProsentForEttKvartal>>
) {
    override fun toString(): String = this.sykefravær.entries.joinToString(", ") { "${it.key}=${it.value}" }
}
