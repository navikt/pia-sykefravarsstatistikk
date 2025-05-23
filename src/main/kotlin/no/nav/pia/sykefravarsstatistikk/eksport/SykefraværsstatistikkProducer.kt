package no.nav.pia.sykefravarsstatistikk.eksport

import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Kafka
import no.nav.pia.sykefravarsstatistikk.konfigurasjon.Topic

class SykefraværsstatistikkProducer(
    kafka: Kafka,
    topic: Topic,
) : KafkaProdusent<Kafkamelding>(kafka = kafka, topic = topic) {
    override fun tilKafkaMelding(input: Kafkamelding): Pair<String, String> = input.nøkkel to input.innhold
}
