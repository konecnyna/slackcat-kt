package com.slackcat.database

import org.jetbrains.exposed.sql.Table
import javax.sql.DataSource

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase(
        storageClients: List<Table>,
        databaseConfig: DataSource,
    ) {
        databaseClient.initialize(storageClients, databaseConfig)
    }
}
