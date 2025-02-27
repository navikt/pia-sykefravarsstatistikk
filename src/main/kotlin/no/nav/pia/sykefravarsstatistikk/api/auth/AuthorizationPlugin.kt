package no.nav.pia.sykefravarsstatistikk.api.auth

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient
import no.nav.pia.sykefravarsstatistikk.Systemmiljø
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedIkkeTilgangTilOrg
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedOkKall
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUgyldigOrgnummer
import no.nav.pia.sykefravarsstatistikk.api.auditlog.auditLogVedUkjentOrgnummer
import no.nav.pia.sykefravarsstatistikk.domene.OverordnetEnhet
import no.nav.pia.sykefravarsstatistikk.domene.Underenhet
import no.nav.pia.sykefravarsstatistikk.exceptions.UgyldigForespørselException
import no.nav.pia.sykefravarsstatistikk.http.hentToken
import no.nav.pia.sykefravarsstatistikk.http.orgnr
import no.nav.pia.sykefravarsstatistikk.http.tokenSubject

@Suppress("ktlint:standard:function-naming")
fun AltinnAuthorizationPlugin(
    altinnKlient: AltinnrettigheterProxyKlient,
    enhetsregisteretService: EnhetsregisteretService,
) = createRouteScopedPlugin(
    name = "AuthorizationPlugin",
) {
    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val fnr = call.request.tokenSubject()
            val token = call.request.hentToken()
            val virksomheterVedkommendeHarTilgangTil = altinnKlient
                .hentEnkelrettighetVirksomheter(
                    token = TokenExchanger.exchangeToken(
                        token = token,
                        audience = Systemmiljø.altinnRettigheterProxyClientId,
                    ),
                    subject = fnr,
                )
            val virksomheterBrukerHarTilknytningTil = altinnKlient
                .hentTilknyttedeVirksomheter(
                    token = TokenExchanger.exchangeToken(
                        token = token,
                        audience = Systemmiljø.altinnRettigheterProxyClientId,
                    ),
                    subject = fnr,
                )

            val orgnr = kotlin.runCatching { call.orgnr }.getOrNull()
            if (orgnr.isNullOrEmpty()) {
                call.auditLogVedUkjentOrgnummer(fnr, virksomheterVedkommendeHarTilgangTil)
                throw UgyldigForespørselException("Manglende parameter 'orgnr'")
            }

            if (!orgnr.erEtOrgNummer()) {
                call.auditLogVedUgyldigOrgnummer(fnr, orgnr, virksomheterVedkommendeHarTilgangTil)
                throw UgyldigForespørselException("Ugyldig orgnummer 'orgnr'")
            }

            val underenhet: Underenhet = enhetsregisteretService.hentUnderEnhet(orgnr)
                .getOrElse { return@on call.respond(it.httpStatusCode, it.feilmelding) }

            val overordnetEnhet: OverordnetEnhet = enhetsregisteretService.hentEnhet(underenhet.overordnetEnhetOrgnr)
                .getOrElse { return@on call.respond(it.httpStatusCode, it.feilmelding) }

            val harTilgangTilOrgnr = virksomheterBrukerHarTilknytningTil
                .any { it.organizationNumber == underenhet.orgnr }
            val harEnkeltTilgang = virksomheterVedkommendeHarTilgangTil
                .any { it.organizationNumber == underenhet.orgnr }
            val harEnkeltTilgangOverordnetEnhet = virksomheterVedkommendeHarTilgangTil
                .any { it.organizationNumber == overordnetEnhet.orgnr }

            if (!harTilgangTilOrgnr) {
                call.respond(status = HttpStatusCode.Forbidden, "Bruker har ikke tilgang til virksomheten")
                    .also { call.auditLogVedIkkeTilgangTilOrg(fnr, orgnr, virksomheterVedkommendeHarTilgangTil) }
            } else {
                call.auditLogVedOkKall(fnr, orgnr, virksomheterVedkommendeHarTilgangTil)
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
