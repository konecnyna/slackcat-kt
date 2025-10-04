package com.slackcat

import com.slackcat.common.DatabaseConfig
import org.apache.commons.dbcp2.BasicDataSource
import javax.sql.DataSource

class DefaultDatasourceFactory {
    fun makeDatabaseSource(engine: Engine, databaseConfig: DatabaseConfig?): DataSource {
        return when (engine) {
            Engine.Slack -> {
                requireNotNull(databaseConfig) { "DatabaseConfig is required for Slack engine" }
                makePostgresSource(databaseConfig)
            }
            Engine.Cli -> makeSqliteSource()
        }
    }

    private fun makeSqliteSource(): DataSource {
        return BasicDataSource().apply {
            url = "jdbc:sqlite:slackcat.db"
            driverClassName = "org.sqlite.JDBC"
        }
    }

    private fun makePostgresSource(databaseConfig: DatabaseConfig): DataSource {
        val databaseUrl = "${databaseConfig.url}/${databaseConfig.name}"
        val dataSource =
            BasicDataSource().apply {
                url = databaseUrl
                username = databaseConfig.username
                password = databaseConfig.password
                driverClassName = "org.postgresql.Driver"
                maxTotal = 10
                maxIdle = 5
                minIdle = 2
            }
        return dataSource
    }
}
