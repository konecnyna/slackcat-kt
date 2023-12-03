package data.database.models

import org.jetbrains.exposed.sql.Table

interface StorageClient {
    // Move table to internal model
    fun provideTable(): Table
}
