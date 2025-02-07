package no.nav.pia.sykefravarsstatistikk.persistering

import kotlinx.datetime.toJavaLocalDateTime
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

    fun insertVirksomhetMetadata(virksomhetMetadataDto: List<VirksomhetMetadataDto>) {
        logger.debug("Lagrer '${virksomhetMetadataDto.size}' metadata for virksomheter")
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                virksomhetMetadataDto.forEach {
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
                                importert = now()
                            """.trimIndent(),
                            mapOf(
                                "orgnr" to it.orgnr,
                                "arstall" to it.årstall,
                                "kvartal" to it.kvartal,
                                "sektor" to it.sektor,
                                "primarnaring" to it.primærnæring,
                                "primarnaringskode" to it.primærnæringskode,
                                "rectype" to it.rectype,
                            ),
                        ).asUpdate,
                    )
                }
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
                                oppdatert_i_dvh,
                                importert
                            )
                            VALUES(
                                :rapport_periode,
                                :offentlig_dato,
                                :oppdatert_i_dvh,
                                :importert
                            )
                            ON CONFLICT (rapport_periode) DO UPDATE SET
                                offentlig_dato = :offentlig_dato,
                                oppdatert_i_dvh = :oppdatert_i_dvh,
                                importert = now()
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
}
