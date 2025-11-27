package no.nav.pia.sykefravarsstatistikk.api

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.api.dto.PubliseringskalenderDto
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.altinnTilgangerContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.TestContainerHelper.Companion.kafkaContainerHelper
import no.nav.pia.sykefravarsstatistikk.helper.withToken
import no.nav.pia.sykefravarsstatistikk.persistering.PubliseringsdatoDto
import kotlin.test.BeforeTest
import kotlin.test.Test

class PubliseringsdatoEndepunktTest {
    @BeforeTest
    fun setup() {
        runBlocking {
            altinnTilgangerContainerHelper.slettAlleRettigheter()
        }
    }

    @Test
    fun `Innlogget bruker får riktig publiseringsdato ift dagens dato`() {
        runBlocking {
            // OBS det finnes allerede en publiseringsdato i DB lagret via script
            kafkaContainerHelper.sendPubliseringsdatoer(
                listOf(
                    PubliseringsdatoDto(
                        rapportPeriode = "202404",
                        offentligDato = LocalDateTime.parse("2025-02-27T08:00:00"),
                        oppdatertIDvh = LocalDateTime.parse("2024-12-02T10:59:59"),
                    ),
                    PubliseringsdatoDto(
                        rapportPeriode = "202501",
                        offentligDato = LocalDateTime.parse("2025-06-01T08:00:00"),
                        oppdatertIDvh = LocalDateTime.parse("2024-12-02T10:59:59"),
                    ),
                    PubliseringsdatoDto(
                        rapportPeriode = "202502",
                        offentligDato = LocalDateTime.parse("2025-09-01T08:00:00"),
                        oppdatertIDvh = LocalDateTime.parse("2024-12-02T10:59:59"),
                    ),
                    PubliseringsdatoDto(
                        rapportPeriode = "202503",
                        offentligDato = LocalDateTime.parse("2025-11-27T08:00:00"),
                        oppdatertIDvh = LocalDateTime.parse("2024-12-02T10:59:59"),
                    ),
                ),
            )

            val response = TestContainerHelper.hentPubliseringsdatoResponse(
                config = withToken(),
            )

            response.status.value shouldBe 200
            val dato = Json.decodeFromString<PubliseringskalenderDto>(response.bodyAsText())
            dato shouldNotBe null
            dato.nestePubliseringsdato shouldBe LocalDate.parse("2026-02-26")
            dato.sistePubliseringsdato shouldBe LocalDate.parse("2025-11-27")
            dato.gjeldendePeriode shouldBe ÅrstallOgKvartal(2025, 3)
        }
    }
}
