package app.modules.kudos

import features.slackcat.models.ColumnType
import features.slackcat.models.Columns
import features.slackcat.models.TableEntity


class KudosDAO {
    data class KudosRow(val id: Int, val userId: String, val count: Int)

    companion object {
        val KudosTable = TableEntity(
            "kudos",
            listOf(
                Columns("id", ColumnType.Int, true),
                Columns("user_id", ColumnType.Text),
                Columns("amount", ColumnType.Int),
            )
        )
    }
//    suspend fun upsertKudos(userId: String): KudosRow {
//        return dbQuery {
//            val existingUser = KudosTable.select { KudosTable.userId eq userId }.singleOrNull()
//            if (existingUser == null) {
//                // User does not exist, insert new user
//                KudosTable.insert {
//                    it[KudosTable.userId] = userId
//                    it[count] = 1
//                }
//            } else {
//                // User exists, increment count
//                KudosTable.update({ KudosTable.userId eq userId }) {
//                    with(SqlExpressionBuilder) {
//                        it.update(count, count + 1)
//                    }
//                }
//            }
//
//            val resultRow = KudosTable.select { KudosTable.userId eq userId }.single()
//            return@dbQuery KudosRow(
//                id = resultRow[KudosTable.id],
//                userId = resultRow[KudosTable.userId],
//                count = resultRow[KudosTable.count],
//            )
//        }
//    }
}
