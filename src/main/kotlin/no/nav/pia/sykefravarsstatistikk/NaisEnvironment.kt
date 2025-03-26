package no.nav.pia.sykefravarsstatistikk

import no.nav.pia.sykefravarsstatistikk.NaisEnvironment.Companion.getEnvVar
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class NaisEnvironment(
    val database: Database = Database(),
) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)

        fun getEnvVar(
            varName: String,
            defaultValue: String? = null,
        ) = System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable $varName")

        fun dagensDato(): LocalDateTime {
            if (System.getenv("NAIS_CLUSTER_NAME") == Clusters.LOKAL.clusterId) {
                val lokalDatoEnvVar: String = getEnvVar("LOKAL_DATO")
                if (lokalDatoEnvVar.isNotEmpty()) {
                    val lokalDato = LocalDateTime.parse(lokalDatoEnvVar)
                    logger.warn("OBS: applikasjon bruker env var 'LOKAL_DATO' med verdi '$lokalDato' som dagens dato")
                    return lokalDato
                }
            }
            val iDag = LocalDateTime.now()
            logger.info("Bruker systemdato '$iDag' som dagens dato")
            return iDag
        }
    }
}

class Database(
    val jdbcUrl: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISTIKK_PIA_SYKEFRAVARSSTATISTIKK_DB_JDBC_URL"),
)

internal object Systemmiljø {
    val tokenxClientId: String by lazy { System.getenv("TOKEN_X_CLIENT_ID") }
    val tokenxIssuer: String by lazy { System.getenv("TOKEN_X_ISSUER") }
    val tokenxJwkPath: String by lazy { System.getenv("TOKEN_X_JWKS_URI") }
    val tokenxPrivateJwk: String by lazy { System.getenv("TOKEN_X_PRIVATE_JWK") }
    val tokenXTokenEndpoint: String by lazy { System.getenv("TOKEN_X_TOKEN_ENDPOINT") }
    val altinnTilgangerProxyUrl: String by lazy { System.getenv("ALTINN_TILGANGER_PROXY_URL") }
    val enhetsregisteretUrl: String by lazy { System.getenv("ENHETSREGISTERET_URL") }
    val cluster: String by lazy { System.getenv("NAIS_CLUSTER_NAME") }
}

enum class Clusters(
    val clusterId: String,
) {
    PROD_GCP("prod-gcp"),
    DEV_GCP("dev-gcp"),
    LOKAL("lokal"),
}
