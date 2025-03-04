package no.nav.pia.sykefravarsstatistikk.api.auth

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedIkkeTilgangTilOrg
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedOkKall
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUgyldigOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUkjentOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.AltinnTilganger
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harEnkeltTilgang
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.harTilgangTilOrgnr
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.virksomheterVedkommendeHarTilgangTil
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.exceptions.UgyldigForespørselException
import no.nav.pia.sykefravarsstatistikk.http.hentToken
import no.nav.pia.sykefravarsstatistikk.http.orgnr
import no.nav.pia.sykefravarsstatistikk.http.tokenSubject

@Suppress("ktlint:standard:function-naming")
fun AltinnAuthorizationPlugin(
    altinnTilgangerService: AltinnTilgangerService,
    enhetsregisteretService: EnhetsregisteretService,
) = createRouteScopedPlugin(
    name = "AuthorizationPlugin",
) {
    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val fnr = call.request.tokenSubject()
            val token = call.request.hentToken()

            val altinnTilganger: AltinnTilganger? =
                altinnTilgangerService.hentAltinnTilganger(token = token).getOrNull()

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

            val overordnetEnhet: OverordnetEnhet = enhetsregisteretService.hentEnhet(underenhet.overordnetEnhetOrgnr)
                .getOrElse { return@on call.respond(it.httpStatusCode, it.feilmelding) }

            val harTilgangTilOrgnr = altinnTilganger.harTilgangTilOrgnr(orgnr = underenhet.orgnr)
            val harEnkeltTilgang =
                altinnTilganger.harEnkeltTilgang(
                    orgnr = underenhet.orgnr,
                    altinn2Tilgang = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
                )
            val harEnkeltTilgangOverordnetEnhet =
                altinnTilganger.harEnkeltTilgang(
                    orgnr = overordnetEnhet.orgnr,
                    altinn2Tilgang = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK,
                )

            if (!harTilgangTilOrgnr) {
                call.respond(status = HttpStatusCode.Forbidden, "Bruker har ikke tilgang til virksomheten")
                    .also {
                        call.auditLogVedIkkeTilgangTilOrg(
                            fnr = fnr,
                            orgnr = orgnr,
                            virksomheter = altinnTilganger.virksomheterVedkommendeHarTilgangTil(),
                        )
                    }
            } else {
                call.auditLogVedOkKall(
                    fnr = fnr,
                    orgnr = orgnr,
                    virksomheter = altinnTilganger.virksomheterVedkommendeHarTilgangTil(),
                )
                call.attributes.put(TilgangerKey, Tilganger(harEnkeltTilgang, harEnkeltTilgangOverordnetEnhet))
                call.attributes.put(UnderenhetKey, underenhet)
                call.attributes.put(OverordnetEnhetKey, overordnetEnhet)
            }
        }
    }
}

private fun String.erEtOrgNummer() = this.matches("^[0-9]{9}$".toRegex())

val TilgangerKey = AttributeKey<Tilganger>("Tilganger")
val UnderenhetKey = AttributeKey<Underenhet>("Underenhet")
val OverordnetEnhetKey = AttributeKey<OverordnetEnhet>("OverordnetEnhet")

data class Tilganger(
    val harEnkeltTilgang: Boolean,
    val harEnkeltTilgangOverordnetEnhet: Boolean,
)
