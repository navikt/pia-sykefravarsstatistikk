package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class MetadataRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun insertVirksomhetMetadata(virksomhetMetadataDto: VirksomhetMetadataDto) {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        """
                        INSERT INTO virksomhet_metadata(
                            orgnr,
                            arstall,
                            kvartal,
                            sektor,
                            primarnaring,
                            primarnaringskode,
                            rectype
                        )
                        VALUES(
                            :orgnr,
                            :arstall,
                            :kvartal,
                            :sektor,
                            :primarnaring,
                            :primarnaringskode,
                            :rectype
                        )
                        ON CONFLICT (orgnr, arstall, kvartal) DO UPDATE SET
                            sektor = :sektor,
                            primarnaring = :primarnaring,
                            primarnaringskode = :primarnaringskode,
                            rectype = :rectype,
                            sist_endret = now()
                        """.trimIndent(),
                        mapOf(
                            "orgnr" to virksomhetMetadataDto.orgnr,
                            "arstall" to virksomhetMetadataDto.årstall,
                            "kvartal" to virksomhetMetadataDto.kvartal,
                            "sektor" to virksomhetMetadataDto.sektor,
                            "primarnaring" to virksomhetMetadataDto.primærnæring,
                            "primarnaringskode" to virksomhetMetadataDto.primærnæringskode,
                            "rectype" to virksomhetMetadataDto.rectype,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    fun insertPubliseringsdato(publiseringsdatoDto: List<PubliseringsdatoDto>) {
        logger.debug("Lagrer '${publiseringsdatoDto.size}' metadata for publiseringsdatoer")
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                publiseringsdatoDto.forEach {
                    tx.run(
                        queryOf(
                            """
                            INSERT INTO publiseringsdatoer(
                                rapport_periode,
                                offentlig_dato,
                                oppdatert_i_dvh
                            )
                            VALUES(
                                :rapport_periode,
                                :offentlig_dato,
                                :oppdatert_i_dvh
                            )
                            ON CONFLICT (rapport_periode) DO UPDATE SET
                                offentlig_dato = :offentlig_dato,
                                oppdatert_i_dvh = :oppdatert_i_dvh,
                                sist_endret = now()
                            """.trimIndent(),
                            mapOf(
                                "rapport_periode" to it.rapportPeriode,
                                "offentlig_dato" to it.offentligDato.toJavaLocalDateTime(),
                                "oppdatert_i_dvh" to it.oppdatertIDvh.toJavaLocalDateTime(),
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
    }

    fun hentPubliseringsdatoer(): List<PubliseringsdatoDto> {
        logger.debug("Henter publiseringsdatoer")
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """
                    SELECT *
                    FROM publiseringsdatoer
                    """.trimIndent(),
                ).map {
                    PubliseringsdatoDto(
                        rapportPeriode = it.string("rapport_periode"),
                        offentligDato = it.localDateTime("offentlig_dato").toKotlinLocalDateTime(),
                        oppdatertIDvh = it.localDateTime("oppdatert_i_dvh").toKotlinLocalDateTime(),
                    )
                }.asList,
            )
        }
    }
}
