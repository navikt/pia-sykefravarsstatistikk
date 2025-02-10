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

        fun kjørerLokalt() = getEnvVar("NAIS_CLUSTER_NAME") == "lokal"
    }
}

class Database(
    val jdbcUrl: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISTIKK_PIA_SYKEFRAVARSSTATISTIKK_DB_JDBC_URL"),
)


