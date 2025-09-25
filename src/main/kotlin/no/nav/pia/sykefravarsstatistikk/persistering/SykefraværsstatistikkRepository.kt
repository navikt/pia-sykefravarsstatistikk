package no.nav.pia.sykefravarsstatistikk.persistering

import ia.felles.definisjoner.bransjer.Bransje
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilNæringskode
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalBransje
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalLand
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæring
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæringskode
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalSektor
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalVirksomhet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class SykefraværsstatistikkRepository(
    private val dataSource: DataSource,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun insertSykefraværsstatistikk(sykefraværsstatistikk: SykefraværsstatistikkDto) {
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.insertStatistikk(
                        sykefraværsstatistikkDto = sykefraværsstatistikk,
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved lagring av sykefraværsstatistikk", e)
            throw e
        }
    }

    fun insertSykefraværsstatistikk(sykefraværsstatistikk: List<SykefraværsstatistikkDto>) {
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
                insertSykefraværsstatistikkMedGradering(
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
                insertSykefraværsstatistikkMedGradering(
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
                insertSykefraværsstatistikkMedGradering(
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

    fun hentSykefraværsstatistikkVirksomhet(orgnr: String): List<UmaskertSykefraværsstatistikkForEttKvartalVirksomhet> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentSykefraværsstatistikk(orgnr)
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av sykefraværsstatistikk for virksomhet $orgnr", e)
            throw e
        }

    fun hentSykefraværsstatistikkNæringskode(næringskode: Næringskode): List<UmaskertSykefraværsstatistikkForEttKvartalNæringskode> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentUmaskertNæringskodestatistikk(næringskode)
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av sykefraværsstatistikk for næring ${næringskode.femsifferIdentifikator}", e)
            throw e
        }

    fun hentSykefraværsstatistikkBransje(bransje: Bransje): List<UmaskertSykefraværsstatistikkForEttKvartalBransje> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentUmaskertBransjestatistikk(bransje)
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av sykefraværsstatistikk for bransje ${bransje.navn}", e)
            throw e
        }

    fun hentSykefraværsstatistikkNæring(næring: Næring): List<UmaskertSykefraværsstatistikkForEttKvartalNæring> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentUmaskertNæringsstatistikk(næring)
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av sykefraværsstatistikk for næring ${næring.navn}", e)
            throw e
        }

    fun hentSykefraværsstatistikkSektor(sektor: Sektor): List<UmaskertSykefraværsstatistikkForEttKvartalSektor> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentUmaskertSektorstatistikk(sektor)
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av sykefraværsstatistikk for sektor $sektor", e)
            throw e
        }

    fun hentSykefraværsstatistikkLand(): List<UmaskertSykefraværsstatistikkForEttKvartalLand> =
        try {
            using(sessionOf(dataSource)) { session ->
                session.transaction { tx ->
                    tx.hentUmaskertLandstatistikk()
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved uthenting av sykefraværsstatistikk for land", e)
            throw e
        }

    private fun Row.tilUmaskertStatistikkVirksomhet(): UmaskertSykefraværsstatistikkForEttKvartalVirksomhet =
        UmaskertSykefraværsstatistikkForEttKvartalVirksomhet(
            orgnr = string("orgnr"),
            årstall = int("arstall"),
            kvartal = int("kvartal"),
            tapteDagsverkGradert = bigDecimal("tapte_dagsverk_gradert"),
            antallPersoner = int("antall_personer"),
            tapteDagsverk = bigDecimal("tapte_dagsverk"),
            muligeDagsverk = bigDecimal("mulige_dagsverk"),
            prosent = bigDecimal("prosent"),
            rectype = string("rectype"),
            opprettet = localDateTime("opprettet"),
        )

    private fun Row.tilUmaskertBransjeStatistikk(): UmaskertSykefraværsstatistikkForEttKvartalBransje =
        UmaskertSykefraværsstatistikkForEttKvartalBransje(
            bransje = Bransje.entries.find { it.navn == string("bransje") }!!,
            årstall = int("arstall"),
            kvartal = int("kvartal"),
            antallPersoner = int("antall_personer"),
            tapteDagsverk = bigDecimal("tapte_dagsverk"),
            muligeDagsverk = bigDecimal("mulige_dagsverk"),
            prosent = bigDecimal("prosent"),
            opprettet = localDateTime("opprettet"),
        )

    private fun Row.tilUmaskertNæringstatistikk(): UmaskertSykefraværsstatistikkForEttKvartalNæring =
        UmaskertSykefraværsstatistikkForEttKvartalNæring(
            næring = Næring(string("naring")),
            årstall = int("arstall"),
            kvartal = int("kvartal"),
            antallPersoner = int("antall_personer"),
            tapteDagsverk = bigDecimal("tapte_dagsverk"),
            muligeDagsverk = bigDecimal("mulige_dagsverk"),
            prosent = bigDecimal("prosent"),
            opprettet = localDateTime("opprettet"),
        )

    private fun Row.tilUmaskertNæringskodestatistikk(): UmaskertSykefraværsstatistikkForEttKvartalNæringskode =
        UmaskertSykefraværsstatistikkForEttKvartalNæringskode(
            næringskode = string("naringskode").tilNæringskode()!!,
            årstall = int("arstall"),
            kvartal = int("kvartal"),
            antallPersoner = int("antall_personer"),
            tapteDagsverk = bigDecimal("tapte_dagsverk"),
            muligeDagsverk = bigDecimal("mulige_dagsverk"),
            prosent = bigDecimal("prosent"),
            opprettet = localDateTime("opprettet"),
        )

    private fun Row.tilUmaskertSektorstatistikk(): UmaskertSykefraværsstatistikkForEttKvartalSektor =
        UmaskertSykefraværsstatistikkForEttKvartalSektor(
            sektor = string("sektor"),
            årstall = int("arstall"),
            kvartal = int("kvartal"),
            antallPersoner = int("antall_personer"),
            tapteDagsverk = bigDecimal("tapte_dagsverk"),
            muligeDagsverk = bigDecimal("mulige_dagsverk"),
            prosent = bigDecimal("prosent"),
            opprettet = localDateTime("opprettet"),
        )

    private fun Row.tilUmaskertLandstatistikk(): UmaskertSykefraværsstatistikkForEttKvartalLand =
        UmaskertSykefraværsstatistikkForEttKvartalLand(
            land = string("land"),
            årstall = int("arstall"),
            kvartal = int("kvartal"),
            antallPersoner = int("antall_personer"),
            tapteDagsverk = bigDecimal("tapte_dagsverk"),
            muligeDagsverk = bigDecimal("mulige_dagsverk"),
            prosent = bigDecimal("prosent"),
            opprettet = localDateTime("opprettet"),
        )

    private fun TransactionalSession.hentUmaskertBransjestatistikk(
        bransje: Bransje,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalBransje> {
        val query =
            """
            SELECT *
            FROM sykefravarsstatistikk_bransje
            WHERE bransje = :bransje
            """.trimIndent()
        return run(
            queryOf(
                query,
                mapOf(
                    "bransje" to bransje.navn,
                ),
            ).map { row -> row.tilUmaskertBransjeStatistikk() }.asList,
        )
    }

    private fun TransactionalSession.hentUmaskertNæringsstatistikk(næring: Næring): List<UmaskertSykefraværsstatistikkForEttKvartalNæring> {
        val query =
            """
            SELECT *
            FROM sykefravarsstatistikk_naring
            WHERE naring = :naring
            """.trimIndent()
        return run(
            queryOf(
                query,
                mapOf(
                    "naring" to næring.tosifferIdentifikator,
                ),
            ).map { row -> row.tilUmaskertNæringstatistikk() }.asList,
        )
    }

    private fun TransactionalSession.hentUmaskertNæringskodestatistikk(
        næringskode: Næringskode,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalNæringskode> {
        val query =
            """
            SELECT *
            FROM sykefravarsstatistikk_naringskode
            WHERE naringskode = :naringskode
            """.trimIndent()
        return run(
            queryOf(
                query,
                mapOf(
                    "naringskode" to næringskode.femsifferIdentifikator,
                ),
            ).map { row -> row.tilUmaskertNæringskodestatistikk() }.asList,
        )
    }

    private fun TransactionalSession.hentUmaskertSektorstatistikk(sektor: Sektor): List<UmaskertSykefraværsstatistikkForEttKvartalSektor> {
        val query =
            """
            SELECT *
            FROM sykefravarsstatistikk_sektor
            WHERE sektor = :sektor
            """.trimIndent()
        return run(
            queryOf(
                query,
                mapOf(
                    "sektor" to sektor.kode,
                ),
            ).map { row -> row.tilUmaskertSektorstatistikk() }.asList,
        )
    }

    private fun TransactionalSession.hentUmaskertLandstatistikk(land: String = "NO"): List<UmaskertSykefraværsstatistikkForEttKvartalLand> {
        val query =
            """
            SELECT *
            FROM sykefravarsstatistikk_land
            WHERE land = :land
            """.trimIndent()
        return run(
            queryOf(
                query,
                mapOf(
                    "land" to land,
                ),
            ).map { row -> row.tilUmaskertLandstatistikk() }.asList,
        )
    }

    private fun TransactionalSession.hentSykefraværsstatistikk(orgnr: String): List<UmaskertSykefraværsstatistikkForEttKvartalVirksomhet> {
        val query =
            """
            SELECT *
            FROM sykefravarsstatistikk_virksomhet
            WHERE orgnr = :orgnr
            """.trimIndent()
        return run(
            queryOf(
                query,
                mapOf(
                    "orgnr" to orgnr,
                ),
            ).map { row -> row.tilUmaskertStatistikkVirksomhet() }.asList,
        )
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

    private fun TransactionalSession.insertSykefraværsstatistikkMedGradering(sykefraværsstatistikkDto: SykefraværsstatistikkDto) {
        val tabellNavn = sykefraværsstatistikkDto.tilTabellNavn()
        val kolonneNavn = sykefraværsstatistikkDto.tilKolonneNavn()

        if (sykefraværsstatistikkDto !is KvartalsvisSykefraværsstatistikkMedGradering) {
            logger.warn("Kan ikke lagre statstitikk med gradering for '${sykefraværsstatistikkDto::class}'")
            return
        }
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
                    tapte_dagsverk_gradert,
                    prosent
                )
                VALUES(
                    :statistikkSpesifikkVerdi,
                    :arstall,
                    :kvartal,
                    :antall_personer,
                    :tapte_dagsverk,
                    :mulige_dagsverk,
                    :tapte_dagsverk_gradert,
                    :prosent
                )
                ON CONFLICT ($kolonneNavn, arstall, kvartal) DO UPDATE SET
                    antall_personer = :antall_personer,
                    tapte_dagsverk = :tapte_dagsverk,
                    mulige_dagsverk = :mulige_dagsverk,
                    tapte_dagsverk_gradert = :tapte_dagsverk_gradert,
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
