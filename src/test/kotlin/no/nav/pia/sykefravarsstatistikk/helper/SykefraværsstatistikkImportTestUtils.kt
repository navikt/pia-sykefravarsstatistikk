package no.nav.pia.sykefravarsstatistikk.helper

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import java.math.BigDecimal

class SykefraværsstatistikkImportTestUtils {
    data class StatistikkGjeldendeKvartal(
        val kategori: Statistikkategori,
        val kode: String,
        val årstall: Int,
        val kvartal: Int,
        val tapteDagsverk: BigDecimal,
        val muligeDagsverk: BigDecimal,
        val prosent: BigDecimal,
        val antallPersoner: Int,
    )

    data class VirksomhetStatistikk(
        val orgnr: String,
        val årstall: Int,
        val kvartal: Int,
        val tapteDagsverk: BigDecimal,
        val muligeDagsverk: BigDecimal,
        val tapteDagsverkGradertSykemelding: BigDecimal,
        val prosent: BigDecimal,
        val antallPersoner: Int,
    )

    data class VirksomhetStatistikkMedVarighet(
        val orgnr: String,
        val årstall: Int,
        val kvartal: Int,
        val tapteDagsverkMedVarighet: List<TapteDagsverkPerVarighet>,
    )

    @Serializable
    data class TapteDagsverkPerVarighet(
        val varighet: String,
        val tapteDagsverk: Double,
    )

    data class JsonMelding(
        val key: JsonKey,
        val value: JsonValue,
    ) {
        constructor(
            kategori: Statistikkategori,
            kode: String,
            årstallOgKvartal: ÅrstallOgKvartal = ÅrstallOgKvartal(2023, 1),
            prosent: Double,
            tapteDagsverk: Double,
            muligeDagsverk: Double,
            antallPersoner: Int,
            tapteDagsverGradert: Double = 0.0,
            tapteDagsverkMedVarighet: List<TapteDagsverkPerVarighet> = emptyList(),
        ) : this(
            JsonKey(
                kategori = kategori,
                kode = kode,
                kvartal = årstallOgKvartal,
            ),
            JsonValue(
                kategori = kategori,
                kode = kode,
                årstall = årstallOgKvartal.årstall,
                kvartal = årstallOgKvartal.kvartal,
                prosent = prosent.toBigDecimal(),
                tapteDagsverk = tapteDagsverk.toBigDecimal(),
                muligeDagsverk = muligeDagsverk.toBigDecimal(),
                tapteDagsverkGradert = tapteDagsverGradert.toBigDecimal(),
                tapteDagsverkPerVarighet = tapteDagsverkMedVarighet,
                antallPersoner = antallPersoner,
            ),
        )

        fun toJsonKey() = key.toJson()

        fun toJsonValue() = value.toJson()
    }

    data class JsonKey(
        val kategori: Statistikkategori,
        val kode: String,
        val kvartal: ÅrstallOgKvartal,
    )

    data class JsonValue(
        val kategori: Statistikkategori,
        val kode: String,
        val årstall: Int,
        val kvartal: Int,
        val prosent: BigDecimal,
        val tapteDagsverk: BigDecimal,
        val muligeDagsverk: BigDecimal,
        val antallPersoner: Int,
        val tapteDagsverkGradert: BigDecimal = 0.toBigDecimal(),
        val tapteDagsverkPerVarighet: List<TapteDagsverkPerVarighet> = emptyList(),
        val rectype: String = "",
    )

    companion object {
        val KVARTAL_2024_3 = ÅrstallOgKvartal(2024, 3)

        infix fun BigDecimal.bigDecimalShouldBe(expected: Double) = this.toDouble().shouldBe(expected)

        private fun Statistikkategori.tilKodenavn() =
            when (this) {
                Statistikkategori.LAND -> "land"
                Statistikkategori.VIRKSOMHET -> "orgnr"
                Statistikkategori.NÆRING -> "næring"
                Statistikkategori.NÆRINGSKODE -> "næringskode"
                Statistikkategori.SEKTOR -> "sektor"
            }

        private fun List<TapteDagsverkPerVarighet>.toJson() = Json.encodeToString(this)

        fun JsonKey.toJson(): String =
            """
            {
              "kategori": "${kategori.name}",
              "kode": "$kode",
              "kvartal": ${kvartal.kvartal},
              "årstall": ${kvartal.årstall}
            }
            """.trimIndent()

        fun JsonValue.toJson(): String =
            when (kategori) {
                Statistikkategori.LAND, Statistikkategori.SEKTOR ->
                    """
                    {
                      "${kategori.tilKodenavn()}": "$kode",
                      "årstall": $årstall,
                      "kvartal": $kvartal,
                      "prosent": ${prosent.toPlainString()},
                      "tapteDagsverk": ${tapteDagsverk.toPlainString()},
                      "muligeDagsverk": ${muligeDagsverk.toPlainString()},
                      "antallPersoner": $antallPersoner
                    }
                    """.trimIndent()

                Statistikkategori.NÆRING ->
                    """
                    {
                      "${kategori.tilKodenavn()}": "$kode",
                        "årstall": $årstall,
                        "kvartal": $kvartal,
                        "prosent": ${prosent.toPlainString()},
                        "tapteDagsverk": ${tapteDagsverk.toPlainString()},
                        "muligeDagsverk": ${muligeDagsverk.toPlainString()},
                        "tapteDagsverkGradert": ${tapteDagsverkGradert.toPlainString()},
                        "tapteDagsverkPerVarighet": ${tapteDagsverkPerVarighet.toJson()},
                        "antallPersoner": $antallPersoner
                    }
                    """.trimIndent()

                Statistikkategori.VIRKSOMHET ->
                    """
                    {
                      "${kategori.tilKodenavn()}": "$kode",
                      "årstall": $årstall,
                      "kvartal": $kvartal,
                      "prosent": ${prosent.toPlainString()},
                      "tapteDagsverk": ${tapteDagsverk.toPlainString()},
                      "muligeDagsverk": ${muligeDagsverk.toPlainString()},
                      "tapteDagsverkGradert": ${tapteDagsverkGradert.toPlainString()},
                      "tapteDagsverkPerVarighet": ${tapteDagsverkPerVarighet.toJson()},
                      "rectype": "$rectype",
                      "antallPersoner": $antallPersoner
                    }
                    """.trimIndent()

                else -> throw IllegalArgumentException("Kategori ikke implementert enda: $kategori")
            }

        fun hentStatistikkGjeldendeKvartal(
            kategori: Statistikkategori,
            verdi: String,
            kvartal: ÅrstallOgKvartal,
            tabellnavn: String,
            kodenavn: String,
        ): StatistikkGjeldendeKvartal {
            val query = """
            select * from $tabellnavn 
             where $kodenavn = '$verdi'
             and arstall = ${kvartal.årstall} and kvartal = ${kvartal.kvartal}
            """.trimMargin()
            TestContainerHelper.postgresContainer.dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                statement.execute(query)
                val rs = statement.resultSet
                rs.next()
                rs.row shouldBe 1
                return StatistikkGjeldendeKvartal(
                    kategori = kategori,
                    kode = rs.getString(kodenavn),
                    årstall = rs.getInt("arstall"),
                    kvartal = rs.getInt("kvartal"),
                    prosent = rs.getBigDecimal("prosent"),
                    tapteDagsverk = rs.getBigDecimal("tapte_dagsverk"),
                    muligeDagsverk = rs.getBigDecimal("mulige_dagsverk"),
                    antallPersoner = rs.getInt("antall_personer"),
                )
            }
        }

        fun hentVirksomhetStatistikk(
            orgnr: String,
            kvartal: ÅrstallOgKvartal,
        ): VirksomhetStatistikk {
            val query = """
            select * from sykefravarsstatistikk_virksomhet 
             where orgnr = '$orgnr'
             and arstall = ${kvartal.årstall} and kvartal = ${kvartal.kvartal}
            """.trimMargin()
            TestContainerHelper.postgresContainer.dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                statement.execute(query)
                val rs = statement.resultSet
                rs.next()
                rs.row shouldBe 1
                return VirksomhetStatistikk(
                    orgnr = rs.getString("orgnr"),
                    årstall = rs.getInt("arstall"),
                    kvartal = rs.getInt("kvartal"),
                    prosent = rs.getBigDecimal("prosent"),
                    tapteDagsverk = rs.getBigDecimal("tapte_dagsverk"),
                    muligeDagsverk = rs.getBigDecimal("mulige_dagsverk"),
                    tapteDagsverkGradertSykemelding = rs.getBigDecimal("tapte_dagsverk_gradert"),
                    antallPersoner = rs.getInt("antall_personer"),
                )
            }
        }

        fun hentVirksomhetStatistikkMedVarighet(
            orgnr: String,
            kvartal: ÅrstallOgKvartal,
        ): VirksomhetStatistikkMedVarighet {
            val query = """
            select * from sykefravarsstatistikk_virksomhet_med_varighet 
             where orgnr = '$orgnr'
             and arstall = ${kvartal.årstall} and kvartal = ${kvartal.kvartal}
            """.trimMargin()
            TestContainerHelper.postgresContainer.dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                statement.execute(query)
                val rs = statement.resultSet
                rs.next()
                rs.row shouldBe 1
                return VirksomhetStatistikkMedVarighet(
                    orgnr = rs.getString("orgnr"),
                    årstall = rs.getInt("arstall"),
                    kvartal = rs.getInt("kvartal"),
                    tapteDagsverkMedVarighet = hentTapteDagsverkMedVarighet(orgnr, kvartal),
                )
            }
        }

        fun hentTapteDagsverkMedVarighet(
            orgnr: String,
            kvartal: ÅrstallOgKvartal,
        ): List<TapteDagsverkPerVarighet> {
            val query = """
            select * from sykefravarsstatistikk_virksomhet_med_varighet 
             where orgnr = '$orgnr'
             and arstall = ${kvartal.årstall} and kvartal = ${kvartal.kvartal}
            """.trimMargin()
            TestContainerHelper.postgresContainer.dataSource.connection.use { connection ->
                val statement = connection.createStatement()
                statement.execute(query)
                val rs = statement.resultSet
                val list = mutableListOf<TapteDagsverkPerVarighet>()
                while (rs.next()) {
                    list.add(
                        TapteDagsverkPerVarighet(
                            varighet = rs.getString("varighet"),
                            tapteDagsverk = rs.getBigDecimal("tapte_dagsverk").toDouble(),
                        ),
                    )
                }
                return list
            }
        }
    }
}
