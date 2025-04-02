package no.nav.pia.sykefravarsstatistikk.konfigurasjon.plugins

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.routing
import no.nav.pia.sykefravarsstatistikk.Systemmiljø
import no.nav.pia.sykefravarsstatistikk.api.aggregering.AggregertStatistikkService
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnOrganisajonerBrukerenHarTilgangTilPlugin
import no.nav.pia.sykefravarsstatistikk.api.auth.AltinnTilgangerService
import no.nav.pia.sykefravarsstatistikk.api.auth.EnhetsregisteretService
import no.nav.pia.sykefravarsstatistikk.api.auth.VerifisertInnloggingPlugin
import no.nav.pia.sykefravarsstatistikk.api.auth.VerifisertEnkelrettighetForOrgnrPlugin
import no.nav.pia.sykefravarsstatistikk.api.organisasjoner
import no.nav.pia.sykefravarsstatistikk.api.publiseringsdato
import no.nav.pia.sykefravarsstatistikk.api.sykefraværsstatistikk
import no.nav.pia.sykefravarsstatistikk.exceptions.IkkeFunnetException
import no.nav.pia.sykefravarsstatistikk.exceptions.UgyldigForespørselException
import no.nav.pia.sykefravarsstatistikk.http.helse
import no.nav.pia.sykefravarsstatistikk.persistering.KvartalsvisSykefraværshistorikkService
import no.nav.pia.sykefravarsstatistikk.persistering.PubliseringsdatoService
import java.net.URI
import java.util.concurrent.TimeUnit

fun Route.medVerifisertEnkeltrettighetForOrgnr(
    enhetsregisteretService: EnhetsregisteretService,
    authorizedRoutes: Route.() -> Unit,
) = (this as RoutingNode).createChild(selector).apply {
    install(
        VerifisertEnkelrettighetForOrgnrPlugin(
            enhetsregisteretService = enhetsregisteretService,
        ),
    )
    authorizedRoutes()
}

fun Route.medOrganisasjonerBrukerenHarTilgangTilIAltinn(
    altinnTilgangerService: AltinnTilgangerService,
    authorizedRoutes: Route.() -> Unit,
) = (this as RoutingNode).createChild(selector).apply {
    install(
        AltinnOrganisajonerBrukerenHarTilgangTilPlugin(
            altinnTilgangerService = altinnTilgangerService,
        ),
    )
    authorizedRoutes()
}

fun Route.medVerifisertInnlogging(authorizedRoutes: Route.() -> Unit) =
    (this as RoutingNode).createChild(selector).apply {
        install(
            VerifisertInnloggingPlugin(),
        )
        authorizedRoutes()
    }

fun Application.configureRouting(
    altinnTilgangerService: AltinnTilgangerService,
    aggregertStatistikkService: AggregertStatistikkService,
    kvartalsvisSykefraværshistorikkService: KvartalsvisSykefraværshistorikkService,
    publiseringsdatoService: PubliseringsdatoService,
    enhetsregisteretService: EnhetsregisteretService,
) {
    routing {
        helse()
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                when (cause) {
                    is IkkeFunnetException -> call.respond(
                        status = HttpStatusCode.NotFound,
                        message = cause.message!!,
                    )

                    is UgyldigForespørselException -> call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = cause.message,
                    )

                    is BadRequestException -> call.respond(
                        status = HttpStatusCode.BadRequest,
                        message = cause.message!!,
                    )

                    else -> {
                        this@configureRouting.log.error("Uhåndtert feil", cause)
                        call.respond(status = HttpStatusCode.InternalServerError, "Uhåndtert feil")
                    }
                }
            }
        }
        val jwkProvider = JwkProviderBuilder(URI(Systemmiljø.tokenxJwkPath).toURL())
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
        install(DoubleReceive)
        install(Authentication) {
            jwt(name = "tokenx") {
                val tokenFortsattGyldigFørUtløpISekunder = 3L
                verifier(jwkProvider, issuer = Systemmiljø.tokenxIssuer) {
                    acceptLeeway(tokenFortsattGyldigFørUtløpISekunder)
                    withAudience(Systemmiljø.tokenxClientId)
                    withClaim("acr") { claim: Claim, _: DecodedJWT ->
                        claim.asString().equals("Level4") ||
                            claim.asString()
                                .equals("idporten-loa-high")
                    }
                    withClaimPresence("sub")
                }
                validate { token ->
                    JWTPrincipal(token.payload)
                }
            }
        }
        install(IgnoreTrailingSlash)
        authenticate("tokenx") {
            medVerifisertInnlogging {
                publiseringsdato(
                    publiseringsdatoService = publiseringsdatoService,
                )
                medOrganisasjonerBrukerenHarTilgangTilIAltinn(
                    altinnTilgangerService = altinnTilgangerService,
                ) {
                    organisasjoner()
                    medVerifisertEnkeltrettighetForOrgnr(
                        enhetsregisteretService = enhetsregisteretService,
                    ) {
                        sykefraværsstatistikk(
                            aggregertStatistikkService = aggregertStatistikkService,
                            kvartalsvisSykefraværshistorikkService = kvartalsvisSykefraværshistorikkService,
                        )
                    }
                }
            }
        }
    }
}
