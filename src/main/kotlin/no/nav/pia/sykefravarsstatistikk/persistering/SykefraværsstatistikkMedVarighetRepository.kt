package no.nav.pia.sykefravarsstatistikk.persistering

import ia.felles.definisjoner.bransjer.Bransje
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Varighetskategori
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/*
   Henter statistikk med varighet (langtid/korttid) for kategorier:
    - Næring
    - Bransje
    - Virksomhet
 */
class SykefraværsstatistikkMedVarighetRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentKorttidsfravær(bransje: Bransje) = hentSykefravær(bransje, Varighetskategori.kortidsvarigheter)

    fun hentKorttidsfravær(næring: Næring) = hentSykefravær(næring, Varighetskategori.kortidsvarigheter)

    fun hentKorttidsfravær(virksomhet: Virksomhet) = hentSykefravær(virksomhet, Varighetskategori.kortidsvarigheter)

    fun hentLangtidsfravær(næring: Næring) = hentSykefravær(næring, Varighetskategori.langtidsvarigheter)

    fun hentLangtidsfravær(bransje: Bransje) = hentSykefravær(bransje, Varighetskategori.langtidsvarigheter)

    fun hentLangtidsfravær(virksomhet: Virksomhet) = hentSykefravær(virksomhet, Varighetskategori.langtidsvarigheter)

    private fun hentSykefravær(
        næring: Næring,
        varigheter: List<Varighetskategori>,
    ): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        try {
            using(sessionOf(dataSource)) { session ->
                val varigheterset = varigheter.toSet()
                val sql = utledStatement(
                    statistikkTabellMedVarighet = "sykefravarsstatistikk_naring_med_varighet",
                    statistikkTabell = "sykefravarsstatistikk_naring",
                    kolonneNavn = "naring",
                )
                val query = queryOf(
                    statement = sql,
                    paramMap = mapOf(
                        "varigheterset" to session.createArrayOf("text", varigheterset),
                        "naring" to næring.tosifferIdentifikator,
                    ),
                ).map { row -> row.tilUmaskertSykefraværForEttKvartal() }.asList
                session.run(query)
            }
        } catch (e: Exception) {
            logger.error(
                "Feil ved uthenting av sykefraværsstatistikk med varighet for naring ${næring.tosifferIdentifikator}",
                e,
            )
            throw e
        }

    private fun hentSykefravær(
        bransje: Bransje,
        varigheter: List<Varighetskategori>,
    ): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        try {
            using(sessionOf(dataSource)) { session ->
                val varigheterset = varigheter.toSet()
                val sql = utledStatement(
                    statistikkTabellMedVarighet = "sykefravarsstatistikk_bransje_med_varighet",
                    statistikkTabell = "sykefravarsstatistikk_bransje",
                    kolonneNavn = "bransje",
                )
                val query = queryOf(
                    statement = sql,
                    paramMap = mapOf(
                        "varigheterset" to session.createArrayOf("text", varigheterset),
                        "bransje" to bransje.navn,
                    ),
                ).map { row -> row.tilUmaskertSykefraværForEttKvartal() }.asList
                session.run(query)
            }
        } catch (e: Exception) {
            logger.error(
                "Feil ved uthenting av sykefraværsstatistikk med varighet for bransje ${bransje.navn}",
                e,
            )
            throw e
        }

    private fun hentSykefravær(
        virksomhet: Virksomhet,
        varigheter: List<Varighetskategori>,
    ): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        try {
            using(sessionOf(dataSource)) { session ->
                val varigheterset = varigheter.toSet()
                val sql = utledStatement(
                    statistikkTabellMedVarighet = "sykefravarsstatistikk_virksomhet_med_varighet",
                    statistikkTabell = "sykefravarsstatistikk_virksomhet",
                    kolonneNavn = "orgnr",
                )
                val orgnr = virksomhet.orgnr
                val query = queryOf(
                    statement = sql,
                    paramMap = mapOf(
                        "varigheterset" to session.createArrayOf("text", varigheterset),
                        "orgnr" to orgnr,
                    ),
                ).map { row -> row.tilUmaskertSykefraværForEttKvartal() }.asList
                session.run(query)
            }
        } catch (e: Exception) {
            logger.error(
                "Feil ved uthenting av sykefraværsstatistikk med varighet for virksomhet ${virksomhet.orgnr}",
                e,
            )
            throw e
        }

    private fun Row.tilUmaskertSykefraværForEttKvartal(): UmaskertSykefraværUtenProsentForEttKvartal =
        UmaskertSykefraværUtenProsentForEttKvartal(
            årstallOgKvartal = ÅrstallOgKvartal(int("arstall"), int("kvartal")),
            dagsverkTeller = bigDecimal("sum_tapte_dagsverk_for_varighet"),
            dagsverkNevner = bigDecimal("mulige_dagsverk"),
            antallPersoner = int("antall_personer"),
        )

    private fun utledStatement(
        statistikkTabellMedVarighet: String,
        statistikkTabell: String,
        kolonneNavn: String,
    ): String {
        val query =
            """
              SELECT 
            STMV.arstall, STMV.kvartal, 
                STMV.$kolonneNavn, 
                ST.mulige_dagsverk,
                ST.antall_personer,
                sum(STMV.tapte_dagsverk) as sum_tapte_dagsverk_for_varighet
              FROM $statistikkTabellMedVarighet STMV
              JOIN $statistikkTabell ST
              ON (ST.$kolonneNavn = STMV.$kolonneNavn and ST.arstall = STMV.arstall and ST.kvartal = STMV.kvartal)
              WHERE 
                STMV.$kolonneNavn = :$kolonneNavn
              AND STMV.varighet IN (select unnest(:varigheterset))
              GROUP BY STMV.arstall, STMV.kvartal, STMV.$kolonneNavn, ST.mulige_dagsverk, ST.antall_personer
              ORDER BY STMV.arstall, STMV.kvartal 
            """.trimIndent()
        return query
    }
}
