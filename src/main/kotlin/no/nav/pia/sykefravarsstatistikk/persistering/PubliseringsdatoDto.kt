package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PubliseringsdatoDto(
    val rapportPeriode: String,
    @Serializable(with = kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer::class)
    val offentligDato: LocalDateTime,
    @Serializable(with = kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer::class)
    val oppdatertIDvh: LocalDateTime,
)

fun String.tilPubliseringsdatoDto(): PubliseringsdatoDto = Json.decodeFromString<PubliseringsdatoDto>(this)
