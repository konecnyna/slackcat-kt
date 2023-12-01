package data.database

import data.database.models.StorageClient

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase(storageClients: List<StorageClient>) {
        databaseClient.initialize(storageClients)
    }
}