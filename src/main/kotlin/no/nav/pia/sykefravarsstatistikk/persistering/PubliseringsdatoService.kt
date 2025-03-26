package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.pia.sykefravarsstatistikk.NaisEnvironment

class PubliseringsdatoService(
    private val publiseringsdatoRepository: MetadataRepository,
) {
    fun hentPubliseringsdatoer() = publiseringsdatoRepository.hentPubliseringsdatoer()

    fun hentPubliseringskalender(publiseringdatoerListe: List<PubliseringsdatoDto>) =
        publiseringdatoerListe.tilPubliseringskalender(dagensDato = dagensDato())

    private fun dagensDato() = NaisEnvironment.dagensDato().toKotlinLocalDateTime()
}
