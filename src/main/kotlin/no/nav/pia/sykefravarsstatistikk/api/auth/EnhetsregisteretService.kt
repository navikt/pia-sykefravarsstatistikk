package no.nav.pia.sykefravarsstatistikk.api.auth

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import no.nav.pia.sykefravarsstatistikk.Systemmiljø
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregEnhetDto
import no.nav.pia.sykefravarsstatistikk.api.dto.BrregUnderenhetDto
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.exceptions.Feil
import no.nav.pia.sykefravarsstatistikk.http.HttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EnhetsregisteretService {
    private val enhetsregisteretApi: String = "${Systemmiljø.enhetsregisteretUrl}/enhetsregisteret/api"
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentUnderEnhet(orgnr: String): Either<Feil, Underenhet> {
        try {
            val url = "$enhetsregisteretApi/underenheter/$orgnr"

            val response: HttpResponse = HttpClient.client.get(url)
            val underenhet = response.body<BrregUnderenhetDto>()
            return underenhet.tilDomene().right()
        } catch (e: Error) {
            logger.error("Feil ved kall til Enhetsregisteret ved henting av enhet '$orgnr'", e)
            return Feil(
                feilmelding = "Feil ved kall til Enhetsregisteret ved henting av enhet '$orgnr'",
                httpStatusCode = HttpStatusCode.InternalServerError,
            ).left()
        }
    }

    suspend fun hentEnhet(orgnr: String): Either<Feil, OverordnetEnhet> {
        try {
            val url = "$enhetsregisteretApi/enheter/$orgnr"
            val response: HttpResponse = HttpClient.client.get(url)
            val overordnetEnhet = response.body<BrregEnhetDto>()
            return overordnetEnhet.tilDomene().right()
        } catch (e: Error) {
            logger.error("Feil ved kall til Enhetsregisteret ved henting av enhet '$orgnr'", e)
            return Feil(
                feilmelding = "Feil ved kall til Enhetsregisteret ved henting av enhet '$orgnr'",
                httpStatusCode = HttpStatusCode.InternalServerError,
            ).left()
        }
    }
}
