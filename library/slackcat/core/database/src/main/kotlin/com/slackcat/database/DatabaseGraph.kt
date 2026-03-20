package com.slackcat.database

import javax.sql.DataSource

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase(databaseConfig: DataSource) {
        databaseClient.connect(databaseConfig)
    }

    fun createTables(storageClients: List<DatabaseTable>) {
        val exposedTables = storageClients.map { it.toExposedTable() }
        databaseClient.createTables(exposedTables)
    }

    fun connectDatabase(
        storageClients: List<DatabaseTable>,
        databaseConfig: DataSource,
    ) {
        val exposedTables = storageClients.map { it.toExposedTable() }
        databaseClient.initialize(exposedTables, databaseConfig)
    }
}
