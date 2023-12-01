package database

import database.models.StorageClient
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseClient {
    companion object {
        val DatabaseName = "slackcat"
    }
    fun initialize(storageClients: List<StorageClient>) {
        Database.connect("jdbc:sqlite:$DatabaseName.db", driver = "org.sqlite.JDBC")
        transaction {
            storageClients.forEach { SchemaUtils.create(it.provideTable())  }
        }
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }