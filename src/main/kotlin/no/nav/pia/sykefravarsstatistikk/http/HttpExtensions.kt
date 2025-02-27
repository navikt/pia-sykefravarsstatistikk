package no.nav.pia.sykefravarsstatistikk.http

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import no.nav.pia.sykefravarsstatistikk.exceptions.UgyldigForespørselException

fun removeBearerPrefix(bearer: String) = bearer.split(" ")[1]

fun ApplicationRequest.hentToken() =
    removeBearerPrefix(
        this.headers[HttpHeaders.Authorization] ?: throw UgyldigForespørselException("No Authorization header found"),
    )

fun ApplicationRequest.tokenSubject() =
    call.principal<JWTPrincipal>()?.get("pid") ?: throw UgyldigForespørselException("pid missing in JWT")

val ApplicationCall.orgnr
    get() = this.parameters["orgnr"] ?: throw UgyldigForespørselException("Manglende parameter 'orgnr'")
