package core.database

import org.jetbrains.exposed.sql.Table

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase(storageClients: List<Table>) {
        databaseClient.initialize(storageClients)
    }
}
