package no.nav.pia.sykefravarsstatistikk.persistering
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MetadataService(
    private val metadataRepository: MetadataRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreVirksomhetMetadata(metadata: VirksomhetMetadataDto) {
        metadataRepository.insertVirksomhetMetadata(metadata)
    }

    fun lagrePubliseringsdato(publiseringdatoer: List<PubliseringsdatoDto>) {
        logger.info("Starter lagring av metadata for publiseringsdatoer, antall metadata som skal lagres: '${publiseringdatoer.size}'\")")
        metadataRepository.insertPubliseringsdato(publiseringdatoer)
    }
}
