package com.slackcat.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class DatabaseClient {
    fun initialize(
        storageClients: List<Table>,
        databaseConfig: DataSource,
    ) {
        Database.connect(databaseConfig)
        transaction {
            storageClients.forEach { SchemaUtils.create(it) }
            // SchemaUtils.create() doesn't add missing indexes to existing tables
            // So we need to use createMissingTablesAndColumns to update schema
            SchemaUtils.createMissingTablesAndColumns(*storageClients.toTypedArray())

            // Drop legacy unique index that conflicts with current schema
            // The current design allows multiple messages per thread (different botMessageTs)
            runMigrations()
        }
    }

    private fun runMigrations() {
        try {
            // Migration: Drop old unique index on thread_ts from kudos_messages table
            // This index was from an earlier schema version and prevents multiple
            // kudos messages in the same thread
            val statement =
                TransactionManager.current().connection.prepareStatement(
                    "DROP INDEX IF EXISTS kudos_messages_thread_ts_unique",
                    false,
                )
            statement.executeUpdate()
        } catch (e: Exception) {
            // Ignore errors if index doesn't exist or database doesn't support IF EXISTS
            val message = "Migration note: Could not drop kudos_messages_thread_ts_unique index"
            println("$message (may not exist): ${e.message}")
        }
    }
}
