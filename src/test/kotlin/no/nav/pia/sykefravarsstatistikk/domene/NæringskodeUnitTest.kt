package no.nav.pia.sykefravarsstatistikk.domene

import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.domene.Næringskode.Companion.tilDomene
import kotlin.test.Test

class NæringskodeUnitTest {
    @Test
    fun `Næringskode skal kunne inneholde punktum eller ikke punktum`() {
        BrregNæringskodeDto(kode = "09.109", "En tilfeldig kode med punktum").tilDomene()?.femsifferIdentifikator shouldBe
            Næringskode(femsifferIdentifikator = "09109").femsifferIdentifikator

        BrregNæringskodeDto(kode = "09109", "En tilfeldig kode UTEN punktum").tilDomene()?.femsifferIdentifikator shouldBe
            Næringskode(femsifferIdentifikator = "09109").femsifferIdentifikator
    }

    @Test
    fun `Skal returnere null ved ugyldig næringskode`() {
        BrregNæringskodeDto(kode = "09.10A", "En ugyldig kode med bokstav").tilDomene() shouldBe null
        BrregNæringskodeDto(kode = "0910", "En ugyldig kode med for få siffer").tilDomene() shouldBe null
        BrregNæringskodeDto(kode = "091090", "En ugyldig kode med for mange siffer").tilDomene() shouldBe null
        BrregNæringskodeDto(kode = "09-109", "En ugyldig kode med bindestrek i stedet for punktum").tilDomene() shouldBe null
        BrregNæringskodeDto(kode = "09*109", "En ugyldig kode med tilfeldig char i stedet for punktum").tilDomene() shouldBe null
    }
}
