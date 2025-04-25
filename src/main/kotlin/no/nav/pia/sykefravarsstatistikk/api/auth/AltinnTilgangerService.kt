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
import no.nav.pia.sykefravarsstatistikk.Metrics
import no.nav.pia.sykefravarsstatistikk.Systemmiljø.altinnTilgangerProxyUrl
import no.nav.pia.sykefravarsstatistikk.Systemmiljø.cluster
import no.nav.pia.sykefravarsstatistikk.domene.AltinnOrganisasjon
import no.nav.pia.sykefravarsstatistikk.exceptions.Feil
import no.nav.pia.sykefravarsstatistikk.http.HttpClient.client
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AltinnTilgangerService {
    private val altinnTilgangerUrl: String = "$altinnTilgangerProxyUrl/altinn-tilganger"

    companion object {
        private val log: Logger = LoggerFactory.getLogger(this::class.java)

        fun AltinnTilganger?.harTilgangTilOrgnr(orgnr: String?): Boolean =
            this?.virksomheterVedkommendeHarTilgangTil()?.contains(orgnr) ?: false

        fun AltinnTilganger?.virksomheterVedkommendeHarTilgangTil() =
            this?.hierarki?.flatMap {
                flatten(it) { o -> o.orgnr }
            }?.toList() ?: emptyList()

        fun AltinnTilganger?.harEnkeltrettighetIAltinn3(
            orgnr: String?,
            enkeltrettighetIAltinn3: String,
        ): Boolean {
            val altinn3Tilgang = harAltinn3Enkeltrettighet(orgnr, enkeltrettighetIAltinn3)
            return altinn3Tilgang
        }

        @Deprecated("Brukes bare til å lage metrikker")
        fun AltinnTilganger?.harEnkeltrettighetIAltinn2(
            orgnr: String?,
            enkeltrettighetIAltinn2: String,
        ): Boolean = harAltinn2Enkeltrettighet(orgnr, enkeltrettighetIAltinn2)


        private fun AltinnTilganger?.harAltinn3Enkeltrettighet(
            orgnr: String?,
            enkeltrettighetIAltinn3: String,
        ): Boolean = this?.orgNrTilTilganger?.get(orgnr)?.contains(enkeltrettighetIAltinn3) ?: false

        @Deprecated("Brukes bare til å lage metrikker")
        private fun AltinnTilganger?.harAltinn2Enkeltrettighet(
            orgnr: String?,
            enkeltrettighetIAltinn2: String,
        ) = this?.orgNrTilTilganger?.get(orgnr)?.contains(enkeltrettighetIAltinn2) ?: false

        fun AltinnTilganger?.altinnOrganisasjonerVedkommendeHarTilgangTil(): List<AltinnOrganisasjon> =
            this?.hierarki?.flatMap {
                flatten(it) { altinnTilgang ->
                    AltinnOrganisasjon(
                        name = altinnTilgang.navn,
                        organizationNumber = altinnTilgang.orgnr,
                        organizationForm = altinnTilgang.organisasjonsform,
                        parentOrganizationNumber = this.finnOverordnetEnhet(altinnTilgang.orgnr) ?: "",
                    )
                }
            }?.toList() ?: emptyList()

        fun AltinnTilganger?.altinnOrganisasjonerVedkommendeHarEnkeltrettighetTil(
            enkeltrettighetIAltinn3: String,
        ): List<AltinnOrganisasjon> =
            this?.hierarki?.flatMap { altinnTilgang ->
                flatten(altinnTilgang) { it }.filter {
                    it.altinn3Tilganger.contains(
                        enkeltrettighetIAltinn3,
                    )
                }.map {
                    AltinnOrganisasjon(
                        name = it.navn,
                        organizationNumber = it.orgnr,
                        organizationForm = it.organisasjonsform,
                        parentOrganizationNumber = this.finnOverordnetEnhet(it.orgnr) ?: "",
                    )
                }
            }?.toList() ?: emptyList()

        fun AltinnTilganger?.finnOverordnetEnhet(orgnr: String): String? {
            val listeAvAltinnTilgangPerOrgnr = this?.listeAvAltinnTilgangPerOrgnr()
            val filtrertMapPåOrgnr: Map<String, List<AltinnTilgang>>? =
                listeAvAltinnTilgangPerOrgnr
                    ?.filter { it.value.any { altinnTilgang -> altinnTilgang.orgnr == orgnr } }
            return if (filtrertMapPåOrgnr?.isEmpty() == true) {
                null
            } else {
                filtrertMapPåOrgnr?.keys?.first()
            }
        }

        private fun AltinnTilganger?.listeAvAltinnTilgangPerOrgnr(): Map<String, List<AltinnTilgang>> =
            this?.hierarki?.flatMap {
                flatten(it) { o: AltinnTilgang -> o.orgnr to o.underenheter }
            }?.toMap() ?: emptyMap()

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
            log.info("henter Altinn tilganger på URL $altinnTilgangerUrl")
            val client = getHttpClient(token)
            val response: HttpResponse = client.post {
                url(altinnTilgangerUrl)
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody("{}")
            }
            response.bodyAsText().tilAltinnTilganger().right()
        } catch (e: Exception) {
            log.error("Feil ved kall til Altinn tilganger", e)
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
