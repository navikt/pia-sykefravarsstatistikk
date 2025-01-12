package no.nav.pia.sykefravarsstatistikk.persistering

import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class SykefraværsstatistikkRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun insertSykefraværsstatistikk(sykefraværsstatistikk: List<SykefraværsstatistikkDto>) {
        logger.debug("Lagrer '${sykefraværsstatistikk.size}' statistikk")
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                sykefraværsstatistikk.forEach {
                    tx.insertStatistikk(
                        sykefraværsstatistikkDto = it,
                    )
                }
            }
        }
    }

    private fun SykefraværsstatistikkDto.tilStatistikkSpesifikkVerdi() =
        when (this) {
            is LandSykefraværsstatistikkDto -> this.land
            is NæringSykefraværsstatistikkDto -> this.næring
            is NæringskodeSykefraværsstatistikkDto -> this.næringskode
            is SektorSykefraværsstatistikkDto -> this.sektor
            is VirksomhetSykefraværsstatistikkDto -> this.orgnr
        }

    private fun SykefraværsstatistikkDto.tilTabellNavn() =
        when (this) {
            is LandSykefraværsstatistikkDto -> "sykefravarsstatistikk_land"
            is NæringSykefraværsstatistikkDto -> "sykefravarsstatistikk_naering"
            is NæringskodeSykefraværsstatistikkDto -> "sykefravarsstatistikk_naeringskode"
            is SektorSykefraværsstatistikkDto -> "sykefravarsstatistikk_sektor"
            is VirksomhetSykefraværsstatistikkDto -> "sykefravarsstatistikk_virksomhet"
        }

    private fun SykefraværsstatistikkDto.tilKolonneNavn() =
        when (this) {
            is LandSykefraværsstatistikkDto -> "land"
            is NæringSykefraværsstatistikkDto -> "naering"
            is NæringskodeSykefraværsstatistikkDto -> "naeringskode"
            is SektorSykefraværsstatistikkDto -> "sektor"
            is VirksomhetSykefraværsstatistikkDto -> "orgnr"
        }

    private fun TransactionalSession.insertStatistikk(sykefraværsstatistikkDto: SykefraværsstatistikkDto) =
        insertIkkeBehandletSykefraværsstatistikk(
            tabellNavn = sykefraværsstatistikkDto.tilTabellNavn(),
            kolonneNavn = sykefraværsstatistikkDto.tilKolonneNavn(),
            sykefraværsstatistikkDto = sykefraværsstatistikkDto,
        )

    private fun TransactionalSession.insertIkkeBehandletSykefraværsstatistikk(
        tabellNavn: String,
        kolonneNavn: String,
        sykefraværsstatistikkDto: SykefraværsstatistikkDto,
    ) {
        val statement =
            """
            INSERT INTO $tabellNavn(
                $kolonneNavn,
                arstall,
                kvartal,
                antall_personer,
                tapte_dagsverk,
                mulige_dagsverk,
                prosent
            )
            VALUES(
                :statistikkSpesifikkVerdi,
                :arstall,
                :kvartal,
                :antall_personer,
                :tapte_dagsverk,
                :mulige_dagsverk,
                :prosent
            )
            ON CONFLICT ($kolonneNavn, arstall, kvartal) DO UPDATE SET
                antall_personer = :antall_personer,
                tapte_dagsverk = :tapte_dagsverk,
                mulige_dagsverk = :mulige_dagsverk,
                prosent = :prosent,
                opprettet = now()
            """.trimIndent()
        run(
            queryOf(
                statement,
                mapOf(
                    "statistikkSpesifikkVerdi" to sykefraværsstatistikkDto.tilStatistikkSpesifikkVerdi(),
                    "arstall" to sykefraværsstatistikkDto.årstall,
                    "kvartal" to sykefraværsstatistikkDto.kvartal,
                    "antall_personer" to sykefraværsstatistikkDto.antallPersoner,
                    "tapte_dagsverk" to sykefraværsstatistikkDto.tapteDagsverk,
                    "mulige_dagsverk" to sykefraværsstatistikkDto.muligeDagsverk,
                    "prosent" to sykefraværsstatistikkDto.prosent,
                ),
            ).asUpdate,
        )
    }
}
