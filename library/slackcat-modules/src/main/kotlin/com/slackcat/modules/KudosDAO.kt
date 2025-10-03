package com.slackcat.modules

import com.slackcat.database.dbUpsert
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table

class KudosDAO {
    data class KudosRow(val id: Int, val userId: String, val count: Int)

    object KudosTable : Table() {
        val id = integer("id").autoIncrement()
        val userId = text("user_id").uniqueIndex()
        val count = integer("count")
        override val primaryKey = PrimaryKey(id)
    }

    suspend fun upsertKudos(userId: String): KudosRow {
        return dbUpsert(
            table = KudosTable,
            keys = arrayOf(KudosTable.userId),
            onUpdate = listOf(
                KudosTable.count to (KudosTable.count + 1)
            ),
            insertBody = {
                it[KudosTable.userId] = userId
                it[KudosTable.count] = 1
            },
            selectWhere = { KudosTable.userId eq userId },
            mapper = { resultRow ->
                KudosRow(
                    id = resultRow[KudosTable.id],
                    userId = resultRow[KudosTable.userId],
                    count = resultRow[KudosTable.count],
                )
            }
        )
    }
}
