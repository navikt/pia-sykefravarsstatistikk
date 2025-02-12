package no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll

import arrow.core.Either
import io.ktor.http.*
import io.ktor.server.application.*

fun <T> ApplicationCall.somInnloggetBruker(
    block: () -> Either<Feil, T>,
): Either<Feil, T> {
    return block()
}

class Feil(
    val feilmelding: String,
    val httpStatusCode: HttpStatusCode,
)
