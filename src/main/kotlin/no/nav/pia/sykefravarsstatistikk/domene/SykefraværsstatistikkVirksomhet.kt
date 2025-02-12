package no.nav.pia.sykefravarsstatistikk.domene

import java.math.BigDecimal

data class SykefraværsstatistikkVirksomhet(
    override val årstall: Int = 0,
    override val kvartal: Int = 0,
    val orgnr: String? = null, // TODO orgnr er vel ikke nullable?
    val varighet: Char? = null,
    val rectype: String,
    override val antallPersoner: Int = 0,
    override val tapteDagsverk: BigDecimal,
    override val muligeDagsverk: BigDecimal
) : Sykefraværsstatistikk
