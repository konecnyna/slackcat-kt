package com.slackcat.modules.storage.kudos

import com.slackcat.database.dbQuery
import com.slackcat.database.dbUpsert
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

class KudosDAO {
    data class KudosRow(val id: Int, val userId: String, val count: Int)

    object KudosTable : Table() {
        val id = integer("id").autoIncrement()
        val userId = text("user_id").uniqueIndex()
        val count = integer("count")
        override val primaryKey = PrimaryKey(id)
    }

    data class KudosMessageRow(val threadTs: String, val botMessageTs: String, val channelId: String)

    object KudosMessageTable : Table("kudos_messages") {
        val threadTs = text("thread_ts").uniqueIndex()
        val botMessageTs = text("bot_message_ts")
        val channelId = text("channel_id")
        override val primaryKey = PrimaryKey(threadTs)
    }

    suspend fun upsertKudos(userId: String): KudosRow {
        return dbUpsert(
            table = KudosTable,
            keys = arrayOf(KudosTable.userId),
            onUpdate =
                listOf(
                    KudosTable.count to (KudosTable.count + 1),
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
            },
        )
    }

    suspend fun getTopKudos(limit: Int = 10): List<KudosRow> {
        return dbQuery {
            KudosTable.selectAll()
                .orderBy(KudosTable.count, SortOrder.DESC)
                .limit(limit)
                .map { resultRow ->
                    KudosRow(
                        id = resultRow[KudosTable.id],
                        userId = resultRow[KudosTable.userId],
                        count = resultRow[KudosTable.count],
                    )
                }
        }
    }

    suspend fun getBotMessageForThread(threadTs: String): KudosMessageRow? {
        return dbQuery {
            KudosMessageTable
                .select { KudosMessageTable.threadTs eq threadTs }
                .map { resultRow ->
                    KudosMessageRow(
                        threadTs = resultRow[KudosMessageTable.threadTs],
                        botMessageTs = resultRow[KudosMessageTable.botMessageTs],
                        channelId = resultRow[KudosMessageTable.channelId],
                    )
                }
                .singleOrNull()
        }
    }

    suspend fun storeBotMessage(
        threadTs: String,
        botMessageTs: String,
        channelId: String,
    ) {
        dbQuery {
            KudosMessageTable.insert {
                it[KudosMessageTable.threadTs] = threadTs
                it[KudosMessageTable.botMessageTs] = botMessageTs
                it[KudosMessageTable.channelId] = channelId
            }
        }
    }
}
