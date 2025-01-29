package no.nav.pia.sykefravarsstatistikk

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun createDataSource(database: Database): DataSource =
    HikariDataSource().apply {
        jdbcUrl = database.jdbcUrl
        maximumPoolSize = 10
        minimumIdle = 1
        idleTimeout = 100000
        connectionTimeout = 100000
        maxLifetime = 300000
    }

fun getFlyway(dataSource: DataSource): Flyway = Flyway.configure().validateMigrationNaming(true).dataSource(dataSource).load()

fun runMigration(dataSource: DataSource) {
    getFlyway(dataSource).migrate()
}
