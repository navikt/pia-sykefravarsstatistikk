package no.nav.pia.sykefravarsstatistikk.persistering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ia.felles.definisjoner.bransjer.Bransje
import io.ktor.http.HttpStatusCode
import no.nav.pia.sykefravarsstatistikk.api.auth.Tilganger
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto
import no.nav.pia.sykefravarsstatistikk.api.dto.KvartalsvisSykefraværshistorikkDto.Companion.tilDto
import no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll.Feil
import no.nav.pia.sykefravarsstatistikk.domene.Næring
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Sektor
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.LAND
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.SEKTOR
import no.nav.pia.sykefravarsstatistikk.domene.Statistikkategori.VIRKSOMHET
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalBransje
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalLand
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalNæring
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalSektor
import no.nav.pia.sykefravarsstatistikk.domene.UmaskertSykefraværsstatistikkForEttKvartalVirksomhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.domene.Virksomhet
import no.nav.pia.sykefravarsstatistikk.domene.ÅrstallOgKvartal
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KvartalsvisSykefraværshistorikkSerivce(
    private val importtidspunktRepository: ImporttidspunktRepository,
    private val sykefraværsstatistikkRepository: SykefraværsstatistikkRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private fun hentSykefraværsstatistikkVirksomhet(
        virksomhet: Virksomhet,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalVirksomhet> {
        logger.info(
            "Henter statistikk for virksomhet med orgnr: '${virksomhet.orgnr}' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}",
        )
        val sykefraværsstatistikkTilVirksomhet = sykefraværsstatistikkRepository.hentSykefraværsstatistikkVirksomhet(
            virksomhet = virksomhet,
        )
        return sykefraværsstatistikkTilVirksomhet.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkBransje(
        bransje: Bransje,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalBransje> {
        logger.info("Henter statistikk for bransje '$bransje' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilBransje = sykefraværsstatistikkRepository.hentSykefraværsstatistikkBransje(
            bransje = bransje,
        )
        return sykefraværsstatistikkTilBransje.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkNæring(
        næring: Næring,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalNæring> {
        logger.info(
            "Henter statistikk for næring '${næring.tosifferIdentifikator}' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}",
        )
        val sykefraværsstatistikkTilNæring =
            sykefraværsstatistikkRepository.hentSykefraværsstatistikkNæring(næring = næring)
        return sykefraværsstatistikkTilNæring.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkSektor(
        sektor: Sektor,
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalSektor> {
        logger.info("Henter statistikk for sektor '$sektor' fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkTilSektor = sykefraværsstatistikkRepository.hentSykefraværsstatistikkSektor(
            sektor = sektor,
        )
        return sykefraværsstatistikkTilSektor.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    private fun hentSykefraværsstatistikkLand(
        førsteÅrstalOgKvartal: ÅrstallOgKvartal,
    ): List<UmaskertSykefraværsstatistikkForEttKvartalLand> {
        logger.info("Henter statistikk for land  fra: ${førsteÅrstalOgKvartal.årstall}K${førsteÅrstalOgKvartal.kvartal}")
        val sykefraværsstatistikkLand = sykefraværsstatistikkRepository.hentSykefraværsstatistikkLand()
        return sykefraværsstatistikkLand.filter {
            ÅrstallOgKvartal(it.årstall, it.kvartal) > førsteÅrstalOgKvartal
        }
    }

    fun hentSykefraværshistorikk(
        overordnetEnhet: OverordnetEnhet,
        underenhet: Underenhet,
        tilganger: Tilganger,
    ): Either<Feil, List<KvartalsvisSykefraværshistorikkDto>> {
        val gjeldendeKvartal = importtidspunktRepository.hentNyesteImporterteKvartal()
        val førsteKvartal = gjeldendeKvartal.minusKvartaler(20)

        val response: MutableList<KvartalsvisSykefraværshistorikkDto> = mutableListOf()

        val umaskertLandstatistikk = hentSykefraværsstatistikkLand(
            førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen landstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }
        response.add(umaskertLandstatistikk.tilDto(type = LAND.name, label = "Norge"))

        val umaskertSektorstatistikk = hentSykefraværsstatistikkSektor(
            sektor = overordnetEnhet.sektor,
            førsteÅrstalOgKvartal = førsteKvartal,
        ).ifEmpty {
            return Feil(
                feilmelding = "Ingen sektorstatistikk funnet",
                httpStatusCode = HttpStatusCode.BadRequest,
            ).left()
        }
        response.add(umaskertSektorstatistikk.tilDto(type = SEKTOR.name, label = overordnetEnhet.sektor.beskrivelse))

        val bransje = underenhet.bransje()
        if (bransje != null) {
            val umaskertBransjestatistikk = hentSykefraværsstatistikkBransje(
                bransje = bransje,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen bransjestatistikk funnet for bransje '${bransje.navn}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            response.add(umaskertBransjestatistikk.tilDto(type = "BRANSJE", label = bransje.navn))
        } else {
            val umaskertNæringsstatistikk = hentSykefraværsstatistikkNæring(
                næring = underenhet.næringskode.næring,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen næringsstatistikk funnet for næring " +
                        "med navn: '${underenhet.næringskode.næring.navn}' " +
                        "og kode: '${underenhet.næringskode.næring.tosifferIdentifikator}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }
            response.add(
                umaskertNæringsstatistikk.tilDto(
                    type = "NÆRING",
                    label = underenhet.næringskode.næring.tosifferIdentifikator,
                ),
            )
        }

        if (tilganger.harEnkeltTilgang) {
            val umaskertVirksomhetsstatistikk = hentSykefraværsstatistikkVirksomhet(
                virksomhet = underenhet,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                return Feil(
                    feilmelding = "Ingen virksomhetsstatistikk funnet for underenhet '${underenhet.orgnr}'",
                    httpStatusCode = HttpStatusCode.BadRequest,
                ).left()
            }

            response.add(umaskertVirksomhetsstatistikk.tilDto(type = VIRKSOMHET.name, label = underenhet.navn))
        }

        val umaskertVirksomhetsstatistikk = if (tilganger.harEnkeltTilgangOverordnetEnhet) {
            hentSykefraværsstatistikkVirksomhet(
                virksomhet = overordnetEnhet,
                førsteÅrstalOgKvartal = førsteKvartal,
            ).ifEmpty {
                logger.info("Ingen virksomhetsstatistikk funnet for overordnet enhet")
                emptyList()
            }
        } else {
            emptyList()
        }
        response.add(
            umaskertVirksomhetsstatistikk.tilDto(type = "OVERORDNET_ENHET", label = overordnetEnhet.navn),
        )

        return response.toList().right()
    }
}
