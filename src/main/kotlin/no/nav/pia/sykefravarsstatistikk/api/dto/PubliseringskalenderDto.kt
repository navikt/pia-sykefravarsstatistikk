package no.nav.pia.sykefravarsstatistikk.api.dto

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal

@Serializable
data class PubliseringskalenderDto(
    val sistePubliseringsdato: LocalDate,
    val nestePubliseringsdato: LocalDate?,
    val gjeldendePeriode: ÅrstallOgKvartal,
)
