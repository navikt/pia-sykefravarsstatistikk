package no.nav.pia.sykefravarsstatistikk.exceptions

import io.ktor.http.HttpStatusCode

class Feil(
    val feilmelding: String,
    val httpStatusCode: HttpStatusCode,
)
