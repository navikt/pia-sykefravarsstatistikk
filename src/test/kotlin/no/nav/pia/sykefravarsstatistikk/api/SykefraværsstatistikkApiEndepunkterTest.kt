package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.inspectors.shouldForOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.helper.AltinnMockHelper.Companion.enVirksomhetIAltinn
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.performGet
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import kotlin.test.Test

class SykefraværsstatistikkApiEndepunkterTest {
    @Test
    fun `Bruker som når et ukjent endepunkt får '404 - Not found' returncode i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enVirksomhetIAltinn.orgnr}/sykefravarshistorikk/alt",
            )
            resultat?.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `Bruker som ikke er innlogget burde få en '401 - Unauthorized' returncode i response`() {
        runBlocking {
            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enVirksomhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
            )
            resultat?.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Innlogget bruker får en 200`() {
        runBlocking {
            val bransje = "Sykehjem"
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
            kafkaContainerHelper.sendVirksomhetsstatistikk()
            // TODO: Får feil om det mangler data, finn en bedre måte å håndtere dette på enn å sende masse data ?

            val resultat = TestContainerHelper.applikasjon.performGet(
                url = "/${enVirksomhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            resultat.shouldNotBeNull()

            resultat.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `Innlogget bruker får kvartalsvis statistikk`() {
        runBlocking {
            val bransje = "Sykehjem" // TODO: Burde ikke være hardkodet i route
            kafkaContainerHelper.sendLandsstatistikk()
            kafkaContainerHelper.sendSektorstatistikk()
            kafkaContainerHelper.sendBransjestatistikk(bransje = bransje)
            kafkaContainerHelper.sendVirksomhetsstatistikk()

            val resultat: HttpResponse? = TestContainerHelper.applikasjon.performGet(
                url = "/${enVirksomhetIAltinn.orgnr}/sykefravarshistorikk/kvartalsvis",
                config = withToken(),
            )

            resultat.shouldNotBeNull()
            resultat.status shouldBe HttpStatusCode.OK

            print(resultat.bodyAsText())

            val aggregertStatistikk: List<KvartalsvisSykefraværshistorikkDto> = resultat.body()

            aggregertStatistikk.size shouldBe 5
            aggregertStatistikk.last().kvartalsvisSykefraværsprosent.size shouldBe 20

            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "OVERORDNET_ENHET" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "VIRKSOMHET" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "BRANSJE" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "LAND" }
            aggregertStatistikk.shouldForOne { statistikk -> statistikk.type shouldBe "SEKTOR" }

            val virksomhet = aggregertStatistikk.first { it.type == "VIRKSOMHET" }
            virksomhet.label shouldBe KvartalsvisSykefraværshistorikkDto.NAVNPÅVIRKSOMHET

            val bransjeStatistikk = aggregertStatistikk.first { it.type == "BRANSJE" }
            bransjeStatistikk.label shouldBe bransje

            val landStatistikk = aggregertStatistikk.first { it.type == "LAND" }
            landStatistikk.label shouldBe "Norge"

            val sektorStatistikk = aggregertStatistikk.first { it.type == "SEKTOR" }
            sektorStatistikk.label shouldBe "1" // TODO: Burde ikke være hardkodet i route

            // Tapte dagsverk
            virksomhet.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 154.5439
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 270744.659570
            landStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 11539578.440000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().tapteDagsverk shouldBe 1275292.330000

            // Mulige dagsverk
            virksomhet.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 761.3
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 4668011.371895
            landStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 180204407.260000
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().muligeDagsverk shouldBe 19790049.740000

            // Prosent
            virksomhet.kvartalsvisSykefraværsprosent.first().prosent shouldBe 28.3
            bransjeStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 5.8
            landStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.4
            sektorStatistikk.kvartalsvisSykefraværsprosent.first().prosent shouldBe 6.3
        }
    }
}
