package no.nav.pia.sykefravarsstatistikk.api.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.authentication
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("ktlint:standard:function-naming")
fun VerifisertInnloggingPlugin() =
    createRouteScopedPlugin(
        name = "VerifisertInnloggingPlugin",
    ) {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
        pluginConfig.apply {
            on(AuthenticationChecked) { call ->
                if (call.authentication.allErrors.isNotEmpty()) {
                    logger.warn("Authentication errors: ${call.authentication.allErrors}")
                    call.respond(
                        status = HttpStatusCode.Unauthorized,
                        ResponseIError(message = "Unauthorized"),
                    )
                    return@on
                }
                if (call.authentication.allFailures.isNotEmpty()) {
                    call.respond(
                        status = HttpStatusCode.Unauthorized,
                        ResponseIError(message = "Unauthorized"),
                    )
                    return@on
                }
            }
        }
    }

@Serializable
data class ResponseIError(
    val message: String,
)
