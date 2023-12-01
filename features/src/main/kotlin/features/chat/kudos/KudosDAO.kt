package features.chat.kudos

import data.database.dbQuery
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*

class KudosDAO {
    @Serializable
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
                    it[this.userId] = userId
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
                count = resultRow[KudosTable.count]
            )
        }
    }


}