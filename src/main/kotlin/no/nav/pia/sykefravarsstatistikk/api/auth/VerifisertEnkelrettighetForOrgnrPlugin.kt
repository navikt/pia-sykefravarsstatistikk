package no.nav.pia.sykefravarsstatistikk.api.auth

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.pia.sykefravarsstatistikk.Systemmiljø
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedIkkeTilgangTilOrg
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedOkKall
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUgyldigOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUkjentOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harEnkeltrettighet
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

                val overordnetEnhet: OverordnetEnhet =
                    enhetsregisteretService.hentEnhet((underenhet as Underenhet.Næringsdrivende).overordnetEnhetOrgnr)
                        .getOrElse { return@on call.respond(it.httpStatusCode, it.feilmelding) }

                val altinnTilganger = call.attributes[AltinnTilgangerKey].altinnTilganger
                val harTilgangTilOrgnr = altinnTilganger.harTilgangTilOrgnr(
                    orgnr = underenhet.orgnr,
                )

                val harEnkeltTilgang = altinnTilganger.harEnkeltrettighet(
                    orgnr = underenhet.orgnr,
                    enkeltrettighetIAltinn2 = Systemmiljø.altinn2EnkeltrettighetKode,
                    enkeltrettighetIAltinn3 = Systemmiljø.altinn3RessursId,
                )

                val harEnkeltTilgangOverordnetEnhet = altinnTilganger.harEnkeltrettighet(
                    orgnr = overordnetEnhet.orgnr,
                    enkeltrettighetIAltinn2 = Systemmiljø.altinn2EnkeltrettighetKode,
                    enkeltrettighetIAltinn3 = Systemmiljø.altinn3RessursId,
                )

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
                            harEnkeltTilgang = harEnkeltTilgang,
                            harEnkeltTilgangOverordnetEnhet = harEnkeltTilgangOverordnetEnhet,
                        ),
                    )
                    call.attributes.put(UnderenhetKey, underenhet)
                    call.attributes.put(OverordnetEnhetKey, overordnetEnhet)
                }
            }
        }
    }

private fun String.erEtOrgNummer() = this.matches("^[0-9]{9}$".toRegex())

val VerifiserteTilgangerKey = AttributeKey<VerifiserteTilganger>("VerifiserteTilganger")
val UnderenhetKey = AttributeKey<Underenhet>("Underenhet")
val OverordnetEnhetKey = AttributeKey<OverordnetEnhet>("OverordnetEnhet")

data class VerifiserteTilganger(
    val harEnkeltTilgang: Boolean,
    val harEnkeltTilgangOverordnetEnhet: Boolean,
)
