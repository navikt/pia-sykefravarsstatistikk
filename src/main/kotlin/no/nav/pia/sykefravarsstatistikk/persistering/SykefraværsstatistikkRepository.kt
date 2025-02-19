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
        logger.info("Lagrer '${sykefraværsstatistikk.size}' statistikk")
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    sykefraværsstatistikk.forEach {
                        tx.insertStatistikk(
                            sykefraværsstatistikkDto = it,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved lagring av sykefraværsstatistikk", e)
            throw e
        }
    }

    private fun SykefraværsstatistikkDto.tilStatistikkSpesifikkVerdi() =
        when (this) {
            is LandSykefraværsstatistikkDto -> this.land
            is NæringSykefraværsstatistikkDto -> this.næring
            is NæringskodeSykefraværsstatistikkDto -> this.næringskode
            is BransjeSykefraværsstatistikkDto -> this.bransje
            is SektorSykefraværsstatistikkDto -> this.sektor
            is VirksomhetSykefraværsstatistikkDto -> this.orgnr
        }

    private fun SykefraværsstatistikkDto.tilTabellNavn() =
        when (this) {
            is LandSykefraværsstatistikkDto -> "sykefravarsstatistikk_land"
            is NæringSykefraværsstatistikkDto -> "sykefravarsstatistikk_naring"
            is NæringskodeSykefraværsstatistikkDto -> "sykefravarsstatistikk_naringskode"
            is BransjeSykefraværsstatistikkDto -> "sykefravarsstatistikk_bransje"
            is SektorSykefraværsstatistikkDto -> "sykefravarsstatistikk_sektor"
            is VirksomhetSykefraværsstatistikkDto -> "sykefravarsstatistikk_virksomhet"
        }

    private fun SykefraværsstatistikkDto.tilKolonneNavn() =
        when (this) {
            is LandSykefraværsstatistikkDto -> "land"
            is NæringSykefraværsstatistikkDto -> "naring"
            is NæringskodeSykefraværsstatistikkDto -> "naringskode"
            is BransjeSykefraværsstatistikkDto -> "bransje"
            is SektorSykefraværsstatistikkDto -> "sektor"
            is VirksomhetSykefraværsstatistikkDto -> "orgnr"
        }

    private fun TransactionalSession.insertStatistikk(sykefraværsstatistikkDto: SykefraværsstatistikkDto) =
        when (sykefraværsstatistikkDto) {
            is LandSykefraværsstatistikkDto -> {
                insertSykefraværsstatistikk(
                    sykefraværsstatistikkDto = sykefraværsstatistikkDto,
                )
            }

            is NæringSykefraværsstatistikkDto -> {
                insertNæringSykefraværsstatistikk(
                    sykefraværsstatistikkDto = sykefraværsstatistikkDto,
                )
                insertTapteDagsverkPerVarighetForKategori(
                    tabellnavn = "sykefravarsstatistikk_naring_med_varighet",
                    kolonnenavn = "naring",
                    verdi = sykefraværsstatistikkDto.næring,
                    årstall = sykefraværsstatistikkDto.årstall,
                    kvartal = sykefraværsstatistikkDto.kvartal,
                    tapteDagsverkPerVarighetListe = sykefraværsstatistikkDto.tapteDagsverkPerVarighet,
                )
            }

            is NæringskodeSykefraværsstatistikkDto -> {
                insertSykefraværsstatistikk(
                    sykefraværsstatistikkDto = sykefraværsstatistikkDto,
                )
                insertTapteDagsverkPerVarighetForKategori(
                    tabellnavn = "sykefravarsstatistikk_naringskode_med_varighet",
                    kolonnenavn = "naringskode",
                    verdi = sykefraværsstatistikkDto.næringskode,
                    årstall = sykefraværsstatistikkDto.årstall,
                    kvartal = sykefraværsstatistikkDto.kvartal,
                    tapteDagsverkPerVarighetListe = sykefraværsstatistikkDto.tapteDagsverkPerVarighet,
                )
            }

            is BransjeSykefraværsstatistikkDto -> {
                insertSykefraværsstatistikk(
                    sykefraværsstatistikkDto = sykefraværsstatistikkDto,
                )
                insertTapteDagsverkPerVarighetForKategori(
                    tabellnavn = "sykefravarsstatistikk_bransje_med_varighet",
                    kolonnenavn = "bransje",
                    verdi = sykefraværsstatistikkDto.bransje,
                    årstall = sykefraværsstatistikkDto.årstall,
                    kvartal = sykefraværsstatistikkDto.kvartal,
                    tapteDagsverkPerVarighetListe = sykefraværsstatistikkDto.tapteDagsverkPerVarighet,
                )
            }

            is SektorSykefraværsstatistikkDto -> {
                insertSykefraværsstatistikk(
                    sykefraværsstatistikkDto = sykefraværsstatistikkDto,
                )
            }

            is VirksomhetSykefraværsstatistikkDto -> {
                insertVirksomhetSykefraværsstatistikk(
                    sykefraværsstatistikkDto = sykefraværsstatistikkDto,
                )
                insertTapteDagsverkPerVarighetForKategori(
                    tabellnavn = "sykefravarsstatistikk_virksomhet_med_varighet",
                    kolonnenavn = "orgnr",
                    verdi = sykefraværsstatistikkDto.orgnr,
                    årstall = sykefraværsstatistikkDto.årstall,
                    kvartal = sykefraværsstatistikkDto.kvartal,
                    tapteDagsverkPerVarighetListe = sykefraværsstatistikkDto.tapteDagsverkPerVarighet,
                )
            }
        }

    private fun TransactionalSession.insertSykefraværsstatistikk(sykefraværsstatistikkDto: SykefraværsstatistikkDto) {
        val tabellNavn = sykefraværsstatistikkDto.tilTabellNavn()
        val kolonneNavn = sykefraværsstatistikkDto.tilKolonneNavn()
        run(
            queryOf(
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
                    sist_endret = now()
                """.trimIndent(),
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

    private fun TransactionalSession.insertNæringSykefraværsstatistikk(sykefraværsstatistikkDto: NæringSykefraværsstatistikkDto) {
        run(
            queryOf(
                """
                INSERT INTO sykefravarsstatistikk_naring(
                    naring,
                    arstall,
                    kvartal,
                    antall_personer,
                    tapte_dagsverk,
                    mulige_dagsverk,
                    tapte_dagsverk_gradert,
                    prosent
                )
                VALUES(
                    :naring,
                    :arstall,
                    :kvartal,
                    :antall_personer,
                    :tapte_dagsverk,
                    :mulige_dagsverk,
                    :tapte_dagsverk_gradert,
                    :prosent
                )
                ON CONFLICT (naring, arstall, kvartal) DO UPDATE SET
                    antall_personer = :antall_personer,
                    tapte_dagsverk = :tapte_dagsverk,
                    mulige_dagsverk = :mulige_dagsverk,
                    tapte_dagsverk_gradert = :tapte_dagsverk_gradert,
                    prosent = :prosent,
                    sist_endret = now()
                """.trimIndent(),
                mapOf(
                    "naring" to sykefraværsstatistikkDto.næring,
                    "arstall" to sykefraværsstatistikkDto.årstall,
                    "kvartal" to sykefraværsstatistikkDto.kvartal,
                    "antall_personer" to sykefraværsstatistikkDto.antallPersoner,
                    "tapte_dagsverk" to sykefraværsstatistikkDto.tapteDagsverk,
                    "mulige_dagsverk" to sykefraværsstatistikkDto.muligeDagsverk,
                    "tapte_dagsverk_gradert" to sykefraværsstatistikkDto.tapteDagsverkGradert,
                    "prosent" to sykefraværsstatistikkDto.prosent,
                ),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.insertVirksomhetSykefraværsstatistikk(sykefraværsstatistikkDto: VirksomhetSykefraværsstatistikkDto) {
        run(
            queryOf(
                """
                INSERT INTO sykefravarsstatistikk_virksomhet(
                    orgnr,
                    arstall,
                    kvartal,
                    antall_personer,
                    tapte_dagsverk,
                    mulige_dagsverk,
                    tapte_dagsverk_gradert,
                    prosent,
                    rectype
                )
                VALUES(
                    :orgnr,
                    :arstall,
                    :kvartal,
                    :antall_personer,
                    :tapte_dagsverk,
                    :mulige_dagsverk,
                    :tapte_dagsverk_gradert,
                    :prosent,
                    :rectype
                )
                ON CONFLICT (orgnr, arstall, kvartal) DO UPDATE SET
                    antall_personer = :antall_personer,
                    tapte_dagsverk = :tapte_dagsverk,
                    mulige_dagsverk = :mulige_dagsverk,
                    tapte_dagsverk_gradert = :tapte_dagsverk_gradert,
                    prosent = :prosent,
                    rectype = :rectype,
                    sist_endret = now()
                """.trimIndent(),
                mapOf(
                    "orgnr" to sykefraværsstatistikkDto.orgnr,
                    "arstall" to sykefraværsstatistikkDto.årstall,
                    "kvartal" to sykefraværsstatistikkDto.kvartal,
                    "antall_personer" to sykefraværsstatistikkDto.antallPersoner,
                    "tapte_dagsverk" to sykefraværsstatistikkDto.tapteDagsverk,
                    "mulige_dagsverk" to sykefraværsstatistikkDto.muligeDagsverk,
                    "tapte_dagsverk_gradert" to sykefraværsstatistikkDto.tapteDagsverkGradert,
                    "prosent" to sykefraværsstatistikkDto.prosent,
                    "rectype" to sykefraværsstatistikkDto.rectype,
                ),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.insertTapteDagsverkPerVarighetForKategori(
        tabellnavn: String,
        kolonnenavn: String,
        verdi: String,
        årstall: Int,
        kvartal: Int,
        tapteDagsverkPerVarighetListe: List<TapteDagsverkPerVarighetDto>,
    ) {
        run(
            queryOf(
                """
                DELETE FROM $tabellnavn
                WHERE $kolonnenavn = :verdi AND arstall = :arstall AND kvartal = :kvartal
                """.trimIndent(),
                mapOf(
                    "verdi" to verdi,
                    "arstall" to årstall,
                    "kvartal" to kvartal,
                ),
            ).asUpdate,
        )
        tapteDagsverkPerVarighetListe.forEach { tapteDagsverkPerVarighet ->
            run(
                queryOf(
                    """
                    INSERT INTO $tabellnavn(
                        $kolonnenavn,
                        arstall,
                        kvartal,
                        varighet,
                        tapte_dagsverk
                    )
                    VALUES(
                        :verdi,
                        :arstall,
                        :kvartal,
                        :varighet,
                        :tapte_dagsverk
                    )
                    """.trimIndent(),
                    mapOf(
                        "verdi" to verdi,
                        "arstall" to årstall,
                        "kvartal" to kvartal,
                        "varighet" to tapteDagsverkPerVarighet.varighet,
                        "tapte_dagsverk" to tapteDagsverkPerVarighet.tapteDagsverk,
                    ),
                ).asUpdate,
            )
        }
    }
}
