package com.slackcat.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseClient {
    companion object {
        const val DatabaseName = "slackcat"
    }

    fun initialize(storageClients: List<Table>) {
        Database.connect("jdbc:sqlite:$DatabaseName.db", driver = "org.sqlite.JDBC")
        transaction {
            storageClients.forEach { SchemaUtils.create(it) }
        }
    }
}

