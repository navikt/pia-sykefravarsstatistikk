package no.nav.pia.sykefravarsstatistikk.domene

@Suppress("ktlint:standard:enum-wrapping")
enum class Varighetskategori(
    val kode: Char?,
) {
    @Suppress("ktlint:standard:enum-entry-name-case")
    _1_DAG_TIL_7_DAGER('A'),

    @Suppress("ktlint:standard:enum-entry-name-case")
    _8_DAGER_TIL_16_DAGER('B'),

    @Suppress("ktlint:standard:enum-entry-name-case")
    _17_DAGER_TIL_8_UKER('C'),

    @Suppress("ktlint:standard:enum-entry-name-case")
    _8_UKER_TIL_20_UKER('D'),

    @Suppress("ktlint:standard:enum-entry-name-case")
    _20_UKER_TIL_39_UKER('E'),

    @Suppress("ktlint:standard:enum-entry-name-case")
    MER_ENN_39_UKER('F'),

    @Suppress("ktlint:standard:enum-entry-name-case")
    TOTAL('X'),
    ;

    override fun toString(): String = kode.toString()

    companion object {
        val kortidsvarigheter = listOf(
            TOTAL,
            _1_DAG_TIL_7_DAGER,
            _8_DAGER_TIL_16_DAGER,
        )
        val langtidsvarigheter = listOf(
            TOTAL,
            _17_DAGER_TIL_8_UKER,
            _8_UKER_TIL_20_UKER,
            _20_UKER_TIL_39_UKER,
            MER_ENN_39_UKER,
        )
    }
}
