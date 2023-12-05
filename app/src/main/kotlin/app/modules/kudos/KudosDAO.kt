package app.modules.kudos

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Table.Dual.autoIncrement
import org.jetbrains.exposed.sql.Table.Dual.integer
import org.jetbrains.exposed.sql.Table.Dual.text
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class KudosDAO {
    data class KudosRow(val id: Int, val userId: String, val count: Int)

    object KudosTable : Table() {
        val id = integer("id").autoIncrement()
        val userId = text("user_id")
        val count = integer("count")
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun upsertKudos(userId: String): KudosRow {
        return dbQuery {
            val existingUser = KudosTable.select { KudosTable.userId eq userId }.singleOrNull()
            if (existingUser == null) {
                // User does not exist, insert new user
                KudosTable.insert {
                    it[KudosTable.userId] = userId
                    it[count] = 1
                }
            } else {
                // User exists, increment count
                KudosTable.update({ KudosTable.userId eq userId }) {
                    with(SqlExpressionBuilder) {
                        it.update(count, count + 1)
                    }
                }
            }

            val resultRow = KudosTable.select { KudosTable.userId eq userId }.single()
            return@dbQuery KudosRow(
                id = resultRow[KudosTable.id],
                userId = resultRow[KudosTable.userId],
                count = resultRow[KudosTable.count],
            )
        }
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
