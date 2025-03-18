package no.nav.pia.sykefravarsstatistikk.helper

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.id.ClientID
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.util.UUID

class AuthContainerHelper(
    network: Network = Network.newNetwork(),
) {
    private val networkAlias = "authserver"
    private val issuerName = "default"
    private val baseEndpointUrl = "http://$networkAlias:6969"
    private val oAuth2Config = OAuth2Config()

    val authContainer = GenericContainer(DockerImageName.parse("ghcr.io/navikt/mock-oauth2-server:2.1.10"))
        .withNetwork(network)
        .withNetworkAliases(networkAlias)
        .withExposedPorts(6969)
        .withEnv(
            mapOf(
                "SERVER_PORT" to "6969",
                "TZ" to "Europe/Oslo",
            ),
        )
        .waitingFor(Wait.forHttp("/default/.well-known/openid-configuration").forStatusCode(200))
        .apply { start() }

    internal fun issueToken(
        issuerId: String = issuerName,
        subject: String = UUID.randomUUID().toString(),
        audience: String,
        claims: Map<String, Any> = emptyMap(),
        expiry: Long = 3600,
    ): SignedJWT {
        val issuerUrl = "$baseEndpointUrl/$issuerName"
        val tokenCallback = DefaultOAuth2TokenCallback(
            issuerId,
            subject,
            JOSEObjectType.JWT.type,
            listOf(audience),
            claims,
            expiry,
        )

        val tokenRequest = TokenRequest(
            URI.create(baseEndpointUrl),
            ClientSecretBasic(ClientID(issuerName), Secret("secret")),
            AuthorizationCodeGrant(AuthorizationCode(FNR), URI.create("http://localhost")),
            Scope(audience),
        )
        return oAuth2Config.tokenProvider.accessToken(tokenRequest, issuerUrl.toHttpUrl(), tokenCallback, null)
    }

    companion object {
        const val FNR = "12345678901"
    }
}

internal fun withToken(block: HttpRequestBuilder.() -> Unit = {}): HttpRequestBuilder.() -> Unit =
    {
        apply(block)
        header(HttpHeaders.Authorization, "Bearer ${TestContainerHelper.accessToken().serialize()}")
    }

internal fun withoutToken(block: HttpRequestBuilder.() -> Unit = {}): HttpRequestBuilder.() -> Unit =
    {
        apply(block)
        header(HttpHeaders.Authorization, "Bearer 12345678901")
    }
