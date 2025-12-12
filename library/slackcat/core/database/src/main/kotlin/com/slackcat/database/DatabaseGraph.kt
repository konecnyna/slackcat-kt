package com.slackcat.database

import javax.sql.DataSource

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase(
        storageClients: List<DatabaseTable>,
        databaseConfig: DataSource,
    ) {
        // Convert DatabaseTable wrappers to Exposed Table objects
        val exposedTables = storageClients.map { it.toExposedTable() }
        databaseClient.initialize(exposedTables, databaseConfig)
    }
}
