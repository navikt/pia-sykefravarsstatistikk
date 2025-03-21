package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.dto.PubliseringskalenderDto
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal

@Serializable
data class PubliseringsdatoDto(
    val rapportPeriode: String,
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val offentligDato: LocalDateTime,
    @Serializable(with = LocalDateTimeIso8601Serializer::class)
    val oppdatertIDvh: LocalDateTime,
)

fun String.tilPubliseringsdatoDto(): PubliseringsdatoDto = Json.decodeFromString<PubliseringsdatoDto>(this)

fun List<PubliseringsdatoDto>.tilPubliseringskalender(dagensDato: LocalDateTime): PubliseringskalenderDto {
    val sortertPubliseringsdatoer = this.sortedBy { it.rapportPeriode }

    val sistePubdato =
        sortertPubliseringsdatoer.filter { it.offentligDato.date <= dagensDato.date }.maxByOrNull { it.offentligDato.date }

    val nestePubdato = sortertPubliseringsdatoer.firstOrNull { it.offentligDato.date > dagensDato.date }?.offentligDato?.date

    val gjeldendePer = sistePubdato?.let { dto ->
        ÅrstallOgKvartal(
            årstall = dto.rapportPeriode.substring(0, 4).toInt(),
            kvartal = dto.rapportPeriode.substring(4).toInt(),
        )
    }

    return PubliseringskalenderDto(
        sistePubliseringsdato = sistePubdato!!.offentligDato.date,
        nestePubliseringsdato = nestePubdato,
        gjeldendePeriode = gjeldendePer!!,
    )
}
