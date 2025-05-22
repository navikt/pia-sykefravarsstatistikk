package no.nav.pia.sykefravarsstatistikk.eksport

import ia.felles.definisjoner.bransjer.Bransje
import no.nav.pia.sykefravarsstatistikk.persistering.VirksomhetMetadataDto

class VirksomhetMetadataEksportService(
    private val statistikkMetadataVirksomhetProdusent: VirksomhetMetadataProducer,
) {
    fun eksporterVirksomhetMetadata(metadata: VirksomhetMetadataDto) {
        statistikkMetadataVirksomhetProdusent.sendPåKafka(
            input = VirksomhetMetadataKafkamelding(
                orgnr = metadata.orgnr,
                årstall = metadata.årstall,
                kvartal = metadata.kvartal,
                sektor = metadata.sektor,
                næring = metadata.primærnæring ?: "",
                næringskode = metadata.primærnæringskode ?: "",
                bransje = if (metadata.primærnæringskode.isNullOrEmpty()) {
                    null
                } else {
                    Bransje.fra(næringskode = metadata.primærnæringskode)?.name
                },
            ),
        )
    }
}
