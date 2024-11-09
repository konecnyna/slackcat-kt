package com.slackcat.app

import org.apache.commons.dbcp2.BasicDataSource
import javax.sql.DataSource

class DatasourceFactory {
    enum class Environment { Production, Development }

    fun makeDatabaseSource(env: Environment): DataSource {
        return when (env) {
            Environment.Production -> makePostgresSource()
            Environment.Development -> makeSqliteSource()
        }
    }

    private fun makeSqliteSource(): DataSource {
        return BasicDataSource().apply {
            url = "jdbc:sqlite:slackcat.db"
            driverClassName = "org.sqlite.JDBC"
        }
    }

    private fun makePostgresSource(): DataSource {
        val databaseUrl = "${System.getenv("DATABASE_URL") ?: "jdbc:sqlite:"}/${System.getenv("DATABASE_NAME")}"
        val dataSource = BasicDataSource().apply {
            url = databaseUrl
            username = System.getenv("DATABASE_USER") ?: ""
            password = System.getenv("DATABASE_PASSWORD") ?: ""
            driverClassName = "org.postgresql.Driver"
            maxTotal = 10
            maxIdle = 5
            minIdle = 2
        }
        return dataSource
    }
}