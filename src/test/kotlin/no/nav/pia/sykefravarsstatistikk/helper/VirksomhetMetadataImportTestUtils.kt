package no.nav.pia.sykefravarsstatistikk.helper
import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal

class VirksomhetMetadataImportTestUtils {
    data class VirksomhetMetadataStatistikk(
        val orgnr: String,
        val årstall: Int,
        val kvartal: Int,
        val sektor: String,
        val primærnæring: String?,
        val primærnæringskode: String?,
        val rectype: String,
    )

    data class VirksomhetMetadataJsonMelding(
        val key: VirksomhetMetadataJsonKey,
        val value: VirksomhetMetadataJsonValue,
    ) {
        constructor(
            orgnr: String,
            årstallOgKvartal: ÅrstallOgKvartal = ÅrstallOgKvartal(2023, 1),
            sektor: String = "2",
            primærnæring: String? = "",
            primærnæringskode: String? = "",
            rectype: String = "1",
        ) : this(
            VirksomhetMetadataJsonKey(
                kvartal = årstallOgKvartal,
                orgnr = orgnr,
            ),
            VirksomhetMetadataJsonValue(
                orgnr = orgnr,
                årstall = årstallOgKvartal.årstall,
                kvartal = årstallOgKvartal.kvartal,
                sektor = sektor,
                primærnæring = primærnæring,
                primærnæringskode = primærnæringskode,
                rectype = rectype,
            ),
        )

        fun toJsonKey() = key.toJson()

        fun toJsonValue() = value.toJson()
    }

    data class VirksomhetMetadataJsonKey(
        val kvartal: ÅrstallOgKvartal,
        val orgnr: String,
    )

    data class VirksomhetMetadataJsonValue(
        val orgnr: String,
        val årstall: Int,
        val kvartal: Int,
        val sektor: String,
        val primærnæring: String? = "",
        val primærnæringskode: String? = "",
        val rectype: String = "",
    )

    companion object {
        fun VirksomhetMetadataJsonKey.toJson(): String =
            """
            {
                "årstall":${kvartal.årstall},
                "kvartal":${kvartal.kvartal},
                "orgnr":"$orgnr"
            }
            """.trimIndent()

        fun VirksomhetMetadataJsonValue.toJson(): String =
            """
            {
                "orgnr":"$orgnr",
                "årstall":$årstall,
                "kvartal":$kvartal,
                "sektor":"$sektor",
                "primærnæring":"$primærnæring",
                "primærnæringskode":"$primærnæringskode",
                "rectype":"$rectype"
            }
            """.trimIndent()

        fun hentVirksomhetMetadataStatistikk(
            orgnr: String,
            kvartal: ÅrstallOgKvartal,
        ): VirksomhetMetadataStatistikk {
            val query = """
            select * from virksomhet_metadata 
             where orgnr = '$orgnr'
             and arstall = ${kvartal.årstall} and kvartal = ${kvartal.kvartal}
            """.trimMargin()
            TestContainerHelper.postgresContainerHelper.dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                statement.execute(query)
                val rs = statement.resultSet
                rs.next()
                rs.row shouldBe 1
                return VirksomhetMetadataStatistikk(
                    orgnr = rs.getString("orgnr"),
                    årstall = rs.getInt("arstall"),
                    kvartal = rs.getInt("kvartal"),
                    sektor = rs.getString("sektor"),
                    primærnæring = rs.getString("primarnaring"),
                    primærnæringskode = rs.getString("primarnaringskode"),
                    rectype = rs.getString("rectype"),
                )
            }
        }
    }
}
