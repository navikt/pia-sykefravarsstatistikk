package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.datetime.toKotlinLocalDateTime

class PubliseringsdatoService(
    private val publiseringsdatoRepository: MetadataRepository,
) {
    fun hentPubliseringsdatoer() = publiseringsdatoRepository.hentPubliseringsdatoer()

    fun hentPubliseringskalender(publiseringdatoerListe: List<PubliseringsdatoDto>) =
        publiseringdatoerListe.tilPubliseringskalender(java.time.LocalDateTime.now().toKotlinLocalDateTime())
}
