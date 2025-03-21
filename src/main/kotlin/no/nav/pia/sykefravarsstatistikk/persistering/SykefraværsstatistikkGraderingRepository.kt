package no.nav.pia.sykefravarsstatistikk.persistering

import ia.felles.definisjoner.bransjer.Bransje
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.pia.sykefravarsstatistikk.api.maskering.UmaskertSykefraværUtenProsentForEttKvartal
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/*
   Henter gradert sykefraværsstatistikk for kategorier:
    - Næring
    - Bransje
    - Virksomhet
 */
class SykefraværsstatistikkGraderingRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentForNæring(næring: Næring): List<UmaskertSykefraværUtenProsentForEttKvartal> = hentGradertSykefravær(næring)

    @Deprecated("Bransje har ikke fått enda tapte_dagsverk_gradert")
    fun hentForBransje(bransje: Bransje): List<UmaskertSykefraværUtenProsentForEttKvartal> = emptyList()

    fun hentForVirksomhet(virksomhet: Virksomhet): List<UmaskertSykefraværUtenProsentForEttKvartal> = hentGradertSykefravær(virksomhet)

    private fun hentGradertSykefravær(næring: Næring): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentGradertSykefravær(næring)
                }
            }
        } catch (e: Exception) {
            logger.error(
                "Feil ved uthenting av sykefraværsstatistikk med varighet for næring ${næring.tosifferIdentifikator}",
                e,
            )
            throw e
        }

    @Deprecated("Bransje har ikke fått enda tapte_dagsverk_gradert")
    private fun hentGradertSykefravær(bransje: Bransje): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentGradertSykefravær(bransje)
                }
            }
        } catch (e: Exception) {
            logger.error(
                "Feil ved uthenting av sykefraværsstatistikk med varighet for bransje ${bransje.navn}",
                e,
            )
            throw e
        }

    private fun hentGradertSykefravær(virksomhet: Virksomhet): List<UmaskertSykefraværUtenProsentForEttKvartal> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentGradertSykefravær(virksomhet)
                }
            }
        } catch (e: Exception) {
            logger.error(
                "Feil ved uthenting av sykefraværsstatistikk med varighet for virksomhet ${virksomhet.orgnr}",
                e,
            )
            throw e
        }

    private fun TransactionalSession.hentGradertSykefravær(næring: Næring): List<UmaskertSykefraværUtenProsentForEttKvartal> {
        val query =
            utledQuery(
                statistikkTabell = "sykefravarsstatistikk_naring",
                kolonneNavn = "naring",
            )
        return run(
            queryOf(
                query,
                mapOf(
                    "naring" to næring.tosifferIdentifikator,
                ),
            ).map { row -> row.tilUmaskertSykefraværForEttKvartal() }.asList,
        )
    }

    private fun TransactionalSession.hentGradertSykefravær(bransje: Bransje): List<UmaskertSykefraværUtenProsentForEttKvartal> {
        val query =
            utledQuery(
                statistikkTabell = "sykefravarsstatistikk_bransje",
                kolonneNavn = "bransje",
            )
        return run(
            queryOf(
                query,
                mapOf(
                    "bransje" to bransje.navn,
                ),
            ).map { row -> row.tilUmaskertSykefraværForEttKvartal() }.asList,
        )
    }

    private fun TransactionalSession.hentGradertSykefravær(virksomhet: Virksomhet): List<UmaskertSykefraværUtenProsentForEttKvartal> {
        val query =
            utledQuery(
                statistikkTabell = "sykefravarsstatistikk_virksomhet",
                kolonneNavn = "orgnr",
            )
        return run(
            queryOf(
                query,
                mapOf(
                    "orgnr" to virksomhet.orgnr,
                ),
            ).map { row -> row.tilUmaskertSykefraværForEttKvartal() }.asList,
        )
    }

    private fun Row.tilUmaskertSykefraværForEttKvartal(): UmaskertSykefraværUtenProsentForEttKvartal =
        UmaskertSykefraværUtenProsentForEttKvartal(
            årstallOgKvartal = ÅrstallOgKvartal(int("arstall"), int("kvartal")),
            dagsverkTeller = bigDecimal("tapte_dagsverk_gradert"),
            dagsverkNevner = bigDecimal("tapte_dagsverk"),
            antallPersoner = int("antall_personer"),
        )

    private fun utledQuery(
        statistikkTabell: String,
        kolonneNavn: String,
    ): String {
        val query =
            """
            SELECT 
              arstall, kvartal, 
              $kolonneNavn, 
              tapte_dagsverk,
              tapte_dagsverk_gradert,
              antall_personer
            FROM $statistikkTabell
            WHERE 
              $kolonneNavn = :$kolonneNavn
            ORDER BY arstall, kvartal 
            """.trimIndent()
        return query
    }
}
