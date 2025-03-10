package no.nav.pia.sykefravarsstatistikk.api.auth

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.nav.pia.sykefravarsstatistikk.Systemmiljø.altinnTilgangerProxyUrl
import no.nav.pia.sykefravarsstatistikk.Systemmiljø.cluster
import no.nav.pia.sykefravarsstatistikk.api.tilgangskontroll.Feil
import no.nav.pia.sykefravarsstatistikk.http.HttpClient.client
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AltinnTilgangerService {
    private val altinnTilgangerUrl: String = "$altinnTilgangerProxyUrl/altinn-tilganger"
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val ENKELRETTIGHET_SYKEFRAVÆRSSTATISTIKK = "3403:1"

        fun AltinnTilganger?.harTilgangTilOrgnr(orgnr: String?): Boolean =
            this?.virksomheterVedkommendeHarTilgangTil()?.contains(orgnr) ?: false

        fun AltinnTilganger?.virksomheterVedkommendeHarTilgangTil() =
            this?.hierarki?.flatMap {
                flatten(it) { o -> o.orgnr }
            }?.toList() ?: emptyList()

        fun AltinnTilganger?.harEnkeltTilgang(
            orgnr: String?,
            altinn2Tilgang: String,
        ) = this?.orgNrTilTilganger?.get(orgnr)?.contains(altinn2Tilgang) ?: false

        private fun <T> flatten(
            altinnTilgang: AltinnTilgang,
            mapFn: (AltinnTilgang) -> T,
        ): Set<T> =
            setOf(
                mapFn(altinnTilgang),
            ) + altinnTilgang.underenheter.flatMap { flatten(it, mapFn) }
    }

    private fun getHttpClient(token: String): HttpClient =
        client.config {
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(
                            accessToken = TokenExchanger.exchangeToken(
                                token = token,
                                audience = "$cluster:fager:arbeidsgiver-altinn-tilganger",
                            ),
                            refreshToken = TokenExchanger.exchangeToken(
                                token = token,
                                audience = "$cluster:fager:arbeidsgiver-altinn-tilganger",
                            ),
                        )
                    }
                }
            }
        }

    suspend fun hentAltinnTilganger(token: String): Either<Feil, AltinnTilganger> =
        try {
            logger.info("henter Altinn tilganger på URL $altinnTilgangerUrl")
            val client = getHttpClient(token)
            val response: HttpResponse = client.post {
                url(altinnTilgangerUrl)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody("{}")
            }
            response.bodyAsText().tilAltinnTilganger().right()
        } catch (e: Exception) {
            logger.error("Feil ved kall til Altinn tilganger", e)
            Feil(
                feilmelding = "Feil ved kall til Altinn tilganger",
                httpStatusCode = HttpStatusCode.InternalServerError,
            ).left()
        }

    fun String.tilAltinnTilganger() = Json.decodeFromString<AltinnTilganger>(this)

    @Serializable
    data class AltinnTilgang(
        val orgnr: String,
        val altinn3Tilganger: Set<String>,
        val altinn2Tilganger: Set<String>,
        val underenheter: List<AltinnTilgang>,
        val navn: String,
        val organisasjonsform: String,
    )

    @Serializable
    data class AltinnTilganger(
        val hierarki: List<AltinnTilgang>,
        val orgNrTilTilganger: Map<String, Set<String>>,
        val tilgangTilOrgNr: Map<String, Set<String>>,
        val isError: Boolean,
    )
}
