package no.nav.pia.sykefravarsstatistikk

import no.nav.pia.sykefravarsstatistikk.NaisEnvironment.Companion.getEnvVar

class NaisEnvironment(
    val database: Database = Database(),
) {
    companion object {
        fun getEnvVar(
            varName: String,
            defaultValue: String? = null,
        ) = System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable $varName")
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
    val altinnRettigheterProxyUrl: String by lazy { System.getenv("ALTINN_RETTIGHETER_PROXY_URL") }
    val altinnRettigheterProxyClientId: String by lazy { System.getenv("ALTINN_RETTIGHETER_PROXY_CLIENT_ID") }
    val cluster: String by lazy { System.getenv("NAIS_CLUSTER_NAME") }
}

enum class Clusters(
    val clusterId: String,
) {
    PROD_GCP("prod-gcp"),
    DEV_GCP("dev-gcp"),
    LOKAL("local"),
}
