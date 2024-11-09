package com.slackcat.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseClient {
    companion object {
        val DATABASE_NAME = "slackcat-${System.getenv("ENV")?.lowercase() ?: "development"}"
    }

    fun initialize(storageClients: List<Table>) {
        Database.connect("jdbc:sqlite:$DATABASE_NAME.db", driver = "org.sqlite.JDBC")
        transaction {
            storageClients.forEach { SchemaUtils.create(it) }
        }
    }
}
