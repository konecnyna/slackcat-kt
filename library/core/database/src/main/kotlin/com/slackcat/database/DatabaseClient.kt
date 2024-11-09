package com.slackcat.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseClient {
    fun initialize(storageClients: List<Table>, databaseConfig: DataSource) {
        Database.connect(databaseConfig)
        transaction {
            storageClients.forEach { SchemaUtils.create(it) }
        }
    }
}
