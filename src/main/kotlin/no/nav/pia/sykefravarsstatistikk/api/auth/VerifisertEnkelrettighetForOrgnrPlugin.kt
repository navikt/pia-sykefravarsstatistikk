package no.nav.pia.sykefravarsstatistikk.api.auth

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.pia.sykefravarsstatistikk.Metrics
import no.nav.pia.sykefravarsstatistikk.Systemmiljø
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedIkkeTilgangTilOrg
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedOkKall
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUgyldigOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUkjentOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harEnkeltrettighetIAltinn2
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harEnkeltrettighetIAltinn3
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harTilgangTilOrgnr
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.exceptions.UgyldigForespørselException
import no.nav.pia.sykefravarsstatistikk.http.orgnr
import no.nav.pia.sykefravarsstatistikk.http.tokenSubject

@Suppress("ktlint:standard:function-naming")
fun VerifisertEnkelrettighetForOrgnrPlugin(enhetsregisteretService: EnhetsregisteretService) =
    createRouteScopedPlugin(
        name = "VerifisertEnkelrettighetForOrgnrPlugin",
    ) {
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val fnr = call.request.tokenSubject()

                val orgnr: String? = kotlin.runCatching { call.orgnr }.getOrNull()
                if (orgnr.isNullOrEmpty()) {
                    call.auditLogVedUkjentOrgnummer(fnr)
                    throw UgyldigForespørselException("Manglende parameter 'orgnr'")
                }

                if (!orgnr.erEtOrgNummer()) {
                    call.auditLogVedUgyldigOrgnummer(fnr, orgnr)
                    throw UgyldigForespørselException("Ugyldig orgnummer 'orgnr'")
                }

                val underenhet: Underenhet = enhetsregisteretService.hentUnderEnhet(orgnr)
                    .getOrElse { return@on call.respond(it.httpStatusCode, it.feilmelding) }

                val altinnTilganger = call.attributes[AltinnTilgangerKey].altinnTilganger
                val harTilgangTilOrgnr = altinnTilganger.harTilgangTilOrgnr(
                    orgnr = underenhet.orgnr,
                )

                val harEnkeltRettighetTilUnderenhet = altinnTilganger.harEnkeltrettighetIAltinn3(
                    orgnr = underenhet.orgnr,
                    enkeltrettighetIAltinn3 = Systemmiljø.altinn3RessursId,
                ).also {
                    metricsCountAltinnTilgang(
                        altinnTilganger = altinnTilganger,
                        orgnr = underenhet.orgnr,
                        harEnkeltRettighetIAtinn3 = it
                    )
                }

                val overordnetEnhet: OverordnetEnhet =
                    enhetsregisteretService.hentEnhet(underenhet.overordnetEnhetOrgnr)
                        .getOrElse { return@on call.respond(it.httpStatusCode, it.feilmelding) }

                val harEnkeltRettighetTilOverordnetEnhet = altinnTilganger.harEnkeltrettighetIAltinn3(
                    orgnr = overordnetEnhet.orgnr,
                    enkeltrettighetIAltinn3 = Systemmiljø.altinn3RessursId,
                ).also {
                    metricsCountAltinnTilgang(
                        altinnTilganger = altinnTilganger,
                        orgnr = overordnetEnhet.orgnr,
                        harEnkeltRettighetIAtinn3 = it
                    )
                }

                if (!harTilgangTilOrgnr) {
                    call.respond(
                        status = HttpStatusCode.Forbidden,
                        ResponseIError(message = "You don't have access to this resource"),
                    )
                        .also {
                            call.auditLogVedIkkeTilgangTilOrg(
                                fnr = fnr,
                                orgnr = orgnr,
                            )
                        }
                } else {
                    call.auditLogVedOkKall(
                        fnr = fnr,
                        orgnr = orgnr,
                    )
                    call.attributes.put(
                        VerifiserteTilgangerKey,
                        VerifiserteTilganger(
                            harEnkeltTilgang = harEnkeltRettighetTilUnderenhet,
                            harEnkeltTilgangOverordnetEnhet = harEnkeltRettighetTilOverordnetEnhet,
                        ),
                    )
                    call.attributes.put(UnderenhetKey, underenhet)
                    call.attributes.put(OverordnetEnhetKey, overordnetEnhet)
                }
            }
        }
    }

private fun metricsCountAltinnTilgang(
    altinnTilganger: AltinnTilgangerService.AltinnTilganger?,
    orgnr: String,
    harEnkeltRettighetIAtinn3: Boolean
) {
    val harFortsattEnkeltRettighetIAltinn2 = altinnTilganger.harEnkeltrettighetIAltinn2(
        orgnr = orgnr,
        enkeltrettighetIAltinn2 = Systemmiljø.altinn2EnkeltrettighetKode,
    )
    Metrics.countAltinnTilgang(
        altinn2 = harFortsattEnkeltRettighetIAltinn2,
        altinn3 = harEnkeltRettighetIAtinn3
    )
}

private fun String.erEtOrgNummer() = this.matches("^[0-9]{9}$".toRegex())

val VerifiserteTilgangerKey = AttributeKey<VerifiserteTilganger>("VerifiserteTilganger")
val UnderenhetKey = AttributeKey<Underenhet>("Underenhet")
val OverordnetEnhetKey = AttributeKey<OverordnetEnhet>("OverordnetEnhet")

data class VerifiserteTilganger(
    val harEnkeltTilgang: Boolean,
    val harEnkeltTilgangOverordnetEnhet: Boolean,
)
