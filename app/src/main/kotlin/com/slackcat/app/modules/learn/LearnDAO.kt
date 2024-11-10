package com.slackcat.app.modules.learn

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

class LearnDAO {

    data class LearnRow(val id: Int, val learnedBy: String, val learnKey: String, val learnText: String)

    object LearnTable : Table() {
        val id = integer("id").autoIncrement()
        val learnedBy = text("learned_by")
        val learnKey = text("learn_key")
        val learnText = text("learn_text")
        override val primaryKey = PrimaryKey(id)
    }

    fun getLearn(key: String, index: Int? = null): Result<LearnRow> {
        return runCatching {
            transaction {
                // Query all rows where learn_key matches the provided key
                val results = LearnTable.select { LearnTable.learnKey eq key }.map {
                    LearnRow(
                        id = it[LearnTable.id],
                        learnedBy = it[LearnTable.learnedBy],
                        learnKey = it[LearnTable.learnKey],
                        learnText = it[LearnTable.learnText]
                    )
                }

                // Determine the item to return based on index or random selection
                when {
                    results.isEmpty() -> throw NoSuchElementException("No learn items found for key: $key")
                    index != null -> results.getOrElse(index) { throw IndexOutOfBoundsException("Index $index out of bounds") }
                    else -> results[Random.nextInt(results.size)]
                }
            }
        }
    }

    fun insertLearn(learnRequest: LearnInsertRow): Boolean {
        return transaction {
            val inserted = LearnTable.insert {
                it[learnedBy] = learnRequest.learnedBy
                it[learnKey] = learnRequest.learnKey
                it[learnText] = learnRequest.learnText
            }
            inserted.insertedCount > 0 // Returns true if insert was successful
        }
    }

    fun getEntriesByLearnKey(key: String): List<LearnRow> {
        return transaction {
            LearnTable.select { LearnTable.learnKey eq key }
                .map {
                    LearnRow(
                        id = it[LearnTable.id],
                        learnedBy = it[LearnTable.learnedBy],
                        learnKey = it[LearnTable.learnKey],
                        learnText = it[LearnTable.learnText]
                    )
                }
        }
    }

    fun removeEntryByIndex(key: String, index: Int): Boolean {
        return transaction {
            val entries = LearnTable.select { LearnTable.learnKey eq key }
                .map { it[LearnTable.id] }

            if (index in entries.indices) {
                // Get the ID of the entry at the specified index
                val entryId = entries[index]
                // Delete the entry
                LearnTable.deleteWhere { LearnTable.id eq entryId } > 0
            } else {
                false // Index out of range
            }
        }
    }
}

data class LearnInsertRow(val learnedBy: String, val learnKey: String, val learnText: String)
