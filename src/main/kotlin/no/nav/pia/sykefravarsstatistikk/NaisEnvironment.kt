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
    val host: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISITKK_DB_HOST"),
    val port: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISITKK_DB_PORT"),
    val username: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISITKK_DB_USERNAME"),
    val password: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISITKK_DB_PASSWORD"),
    val name: String = getEnvVar("NAIS_DATABASE_PIA_SYKEFRAVARSSTATISITKK_DB_DATABASE"),
)


