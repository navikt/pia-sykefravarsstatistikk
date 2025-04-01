package no.nav.pia.sykefravarsstatistikk.api.auth

import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.util.AttributeKey
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.AltinnTilganger
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService.Companion.altinnOrganisasjonerVedkommendeHarTilgangTil
import no.nav.pia.sykefravarsstatistikk.domene.AltinnOrganisasjon
import no.nav.pia.sykefravarsstatistikk.http.hentToken

@Suppress("ktlint:standard:function-naming")
fun AltinnOrganisajonerBrukerenHarTilgangTilPlugin(altinnTilgangerService: AltinnTilgangerService) =
    createRouteScopedPlugin(
        name = "AltinnOrganisajonerBrukerenHarTilgangTilPlugin",
    ) {
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                val token = call.request.hentToken()

                val altinnTilganger: AltinnTilganger? =
                    altinnTilgangerService.hentAltinnTilganger(token = token).getOrNull()
                val altinnOrganisasjonerVedkommendeHarTilgangTil =
                    altinnTilganger.altinnOrganisasjonerVedkommendeHarTilgangTil()
                val altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil =
                    altinnTilganger.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil(
                        enkeltrettighetIAltinn2 = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_2,
                        enkeltrettighetIAltinn3 = ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK_ALTINN_3,
                    )
                call.attributes.put(
                    AltinnTilgangerKey,
                    AltinnTilgangerOnCall(
                        altinnTilganger = altinnTilganger,
                        altinnOrganisasjonerVedkommendeHarTilgangTil = altinnOrganisasjonerVedkommendeHarTilgangTil,
                        altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil = altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil,
                    ),
                )
            }
        }
    }

val AltinnTilgangerKey = AttributeKey<AltinnTilgangerOnCall>("AltinnTilganger")

data class AltinnTilgangerOnCall(
    val altinnTilganger: AltinnTilganger?,
    val altinnOrganisasjonerVedkommendeHarTilgangTil: List<AltinnOrganisasjon>,
    val altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil: List<AltinnOrganisasjon>,
)
