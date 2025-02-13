package no.nav.pia.sykefravarsstatistikk.helper
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import no.nav.pia.sykefravarsstatistikk.persistering.PubliseringsdatoDto

class PubliseringsdatoImportTestUtils {
    data class PubliseringsdatoJsonMelding(
        val key: PubliseringsdatoJsonKey,
        val value: PubliseringsdatoJsonValue,
    ) {
        constructor(
            rapportPeriode: String = "202404",
            offentligDato: LocalDateTime = LocalDateTime.parse("2025-02-27T08:00:00"),
            oppdatertIDvh: LocalDateTime = LocalDateTime.parse("2024-11-19T08:00:00"),
        ) : this(
            PubliseringsdatoJsonKey(
                rapportPeriode = rapportPeriode,
            ),
            PubliseringsdatoJsonValue(
                rapportPeriode = rapportPeriode,
                offentligDato = offentligDato,
                oppdatertIDvh = oppdatertIDvh,
            ),
        )

        fun toJsonKey() = key.toJson()

        fun toJsonValue() = value.toJson()
    }

    data class PubliseringsdatoJsonKey(
        val rapportPeriode: String,
    )

    data class PubliseringsdatoJsonValue(
        val rapportPeriode: String = "202404",
        val offentligDato: LocalDateTime = LocalDateTime.parse("2025-02-27T08:00:00"),
        val oppdatertIDvh: LocalDateTime = LocalDateTime.parse("2024-11-19T08:00:00"),
    )

    companion object {
        fun PubliseringsdatoJsonKey.toJson(): String =
            """
            {
                "rapportPeriode":"$rapportPeriode"
            }
            """.trimIndent()

        fun PubliseringsdatoJsonValue.toJson(): String =
            """
            {
                "rapportPeriode": "$rapportPeriode",
                "offentligDato": "$offentligDato",
                "oppdatertIDvh": "$oppdatertIDvh"
            }
            """.trimIndent()

        fun hentPubliseringsdato(rapportPeriode: String): PubliseringsdatoDto {
            val query = """
            select * from publiseringsdatoer 
             where rapport_periode = '$rapportPeriode'
            """.trimMargin()
            TestContainerHelper.postgresContainerHelper.dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                statement.execute(query)
                val rs = statement.resultSet
                if (!rs.next()) {
                    throw IllegalStateException("Ingen rader funnet for rapportPeriode = $rapportPeriode")
                }
                rs.row shouldBe 1
                return PubliseringsdatoDto(
                    rapportPeriode = rs.getString("rapport_periode"),
                    offentligDato = rs.getTimestamp("offentlig_dato").toLocalDateTime().toKotlinLocalDateTime(),
                    oppdatertIDvh = rs.getTimestamp("oppdatert_i_dvh").toLocalDateTime().toKotlinLocalDateTime(),
                )
            }
        }
    }
}
