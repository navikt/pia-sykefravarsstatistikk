package no.nav.pia.sykefravarsstatistikk.helper

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.shouldBe
import no.nav.pia.sykefravarsstatistikk.runMigration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy

class PostgrestContainerHelper(
    network: Network = Network.newNetwork(),
    log: Logger = LoggerFactory.getLogger(PostgrestContainerHelper::class.java),
) {
    private val postgresNetworkAlias = "postgrescontainer"
    private val piaSykefraværsstatistikkDbName = "pia-sykefravarsstatistikk-container-db"
    private var migreringErKjørt = false
    val postgresContainer: PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:17")
            .withLogConsumer(
                Slf4jLogConsumer(log).withPrefix(postgresNetworkAlias).withSeparateOutputStreams(),
            )
            .withNetwork(network)
            .withNetworkAliases(postgresNetworkAlias)
            .withDatabaseName(piaSykefraværsstatistikkDbName)
            .withCreateContainerCmdModifier { cmd -> cmd.withName("$postgresNetworkAlias-${System.currentTimeMillis()}") }
            .waitingFor(HostPortWaitStrategy()).apply {
                start()
            }

    val dataSource = nyDataSource()

    fun nyDataSource() =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = postgresContainer.jdbcUrl
                username = postgresContainer.username
                password = postgresContainer.password
            },
        ).also {
            if (!migreringErKjørt) {
                runMigration(it)
                migreringErKjørt = true
            }
        }

    fun <T> hentAlleRaderTilEnkelKolonne(sql: String): List<T> {
        dataSource.connection.use { connection ->
            val statement = connection.createStatement()
            statement.execute(sql)
            val rs = statement.resultSet
            val list = mutableListOf<T>()
            while (rs.next()) {
                list.add(rs.getObject(1) as T)
            }
            return list
        }
    }

    fun <T> hentEnkelKolonne(sql: String): T {
        dataSource.connection.use { connection ->
            val statement = connection.createStatement()
            statement.execute(sql)
            val rs = statement.resultSet
            rs.next()
            rs.row shouldBe 1
            return rs.getObject(1) as T
        }
    }

    fun envVars() =
        mapOf(
            "NAIS_DATABASE_PIA_SYKEFRAVARSSTATISTIKK_PIA_SYKEFRAVARSSTATISTIKK_DB_JDBC_URL" to
                "jdbc:postgresql://$postgresNetworkAlias:5432/$piaSykefraværsstatistikkDbName?password=${postgresContainer.password}&user=${postgresContainer.username}",
        )
}
