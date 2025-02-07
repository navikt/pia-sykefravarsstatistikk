package no.nav.pia.sykefravarsstatistikk.persistering
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MetadataService(
    private val metadataRepository: MetadataRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun lagreVirksomhetMetadata(metadata: List<VirksomhetMetadataDto>) {
        logger.info("Starter import av metadata for virksomhet, antall metadata som skal lagres: '${metadata.size}'\")")
        metadataRepository.insertVirksomhetMetadata(metadata)
    }

    fun lagrePubliseringsdato(publiseringdatoer: List<PubliseringsdatoDto>) {
        logger.info("Starter import av metadata for publiseringsdatoer, antall metadata som skal lagres: '${publiseringdatoer.size}'\")")
        metadataRepository.insertPubliseringsdato(publiseringdatoer)
    }
}
