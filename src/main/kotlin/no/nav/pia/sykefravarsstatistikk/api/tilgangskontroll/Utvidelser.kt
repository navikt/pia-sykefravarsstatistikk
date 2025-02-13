package no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll

import arrow.core.Either
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

fun <T> ApplicationCall.somInnloggetBruker(
    block: () -> Either<Feil, T>,
): Either<Feil, T> = block()

class Feil(
    val feilmelding: String,
    val httpStatusCode: HttpStatusCode,
)
