package no.nav.pia.sykefravarsstatistikk

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val NAMESPACE = "pia"

class Metrics {
    companion object {
        val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

        private val altinn3Rettighet: Counter = Counter.builder()
            .name("${NAMESPACE}_sykefravarsstatistikk_altinn_3_enkeltrettighet")
            .help("Bruker fikk tilgang til sykefraværsstatistikk med kun Altinn 3")
            .withoutExemplars().register(appMicrometerRegistry.prometheusRegistry)

        private val altinn2Rettighet: Counter = Counter.builder()
            .name("${NAMESPACE}_sykefravarsstatistikk_altinn_2_enkeltrettighet")
            .help("Bruker fikk tilgang til sykefraværsstatistikk med kun Altinn 2")
            .withoutExemplars().register(appMicrometerRegistry.prometheusRegistry)

        private val altinn2og3Rettighet: Counter = Counter.builder()
            .name("${NAMESPACE}_sykefravarsstatistikk_altinn_2_og_3_enkeltrettighet")
            .help("Bruker fikk tilgang til sykefraværsstatistikk med både Altinn 2 og 3")
            .withoutExemplars().register(appMicrometerRegistry.prometheusRegistry)

        fun countAltinnTilgang(
            altinn2: Boolean,
            altinn3: Boolean,
        ) {
            if (altinn2 && altinn3) {
                altinn2og3Rettighet.inc()
            } else if (altinn2) {
                altinn2Rettighet.inc()
            } else if (altinn3) {
                altinn3Rettighet.inc()
            }
        }
    }
}
