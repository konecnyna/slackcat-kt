package com.slackcat.modules.storage.kudos

import com.slackcat.database.DatabaseTable
import com.slackcat.database.asDatabaseTable
import com.slackcat.database.dbQuery
import com.slackcat.database.dbUpsert
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class KudosDAO(
    private val spamProtectionEnabled: Boolean = true,
) {
    companion object {
        /**
         * Returns the database tables wrapped to hide Exposed implementation.
         * This is used by KudosModule to register tables with the database layer.
         */
        fun getDatabaseTables(): List<DatabaseTable> {
            return listOf(
                KudosTable.asDatabaseTable(),
                KudosMessageTable.asDatabaseTable(),
                KudosTransactionTable.asDatabaseTable(),
            )
        }
    }

    data class KudosRow(val id: Int, val userId: String, val count: Int)

    object KudosTable : Table() {
        val id = integer("id").autoIncrement()
        val userId = text("user_id").uniqueIndex()
        val count = integer("count")
        override val primaryKey = PrimaryKey(id)
    }

    data class KudosMessageRow(
        val threadTs: String,
        val botMessageTs: String,
        val channelId: String,
        val createdAt: Long,
        val expiresAt: Long,
        val userCounts: Map<String, Int> = emptyMap(),
    )

    object KudosMessageTable : Table("kudos_messages") {
        val threadTs = text("thread_ts")
        val botMessageTs = text("bot_message_ts")
        val channelId = text("channel_id")
        val createdAt = long("created_at").default(0L)
        val expiresAt = long("expires_at").default(0L)

        // Stores user_id -> count mapping as JSON string: {"U123": 5, "U456": 3}
        val userCounts = text("user_counts").default("{}")
        override val primaryKey = PrimaryKey(threadTs, botMessageTs)

        init {
            // Index for fetching active message in thread
            index(isUnique = false, threadTs, expiresAt)
        }
    }

    data class KudosTransactionRow(
        val id: Int,
        val giverId: String,
        val recipientId: String,
        val threadTs: String,
        val timestamp: Long,
    )

    object KudosTransactionTable : Table("kudos_transactions") {
        val id = integer("id").autoIncrement()
        val giverId = text("giver_id")
        val recipientId = text("recipient_id")
        val threadTs = text("thread_ts")
        val timestamp = long("timestamp")
        override val primaryKey = PrimaryKey(id)

        init {
            // Composite index for per-thread deduplication
            index(isUnique = true, giverId, recipientId, threadTs)
            // Index for global cooldown checks
            index(isUnique = false, giverId, recipientId, timestamp)
        }
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

    suspend fun getActiveMessageForThread(
        threadTs: String,
        currentTime: Long = System.currentTimeMillis(),
    ): KudosMessageRow? {
        return dbQuery {
            KudosMessageTable
                .select {
                    (KudosMessageTable.threadTs eq threadTs) and
                        (KudosMessageTable.expiresAt greater currentTime)
                }
                .orderBy(KudosMessageTable.createdAt, SortOrder.DESC)
                .limit(1)
                .map { resultRow ->
                    KudosMessageRow(
                        threadTs = resultRow[KudosMessageTable.threadTs],
                        botMessageTs = resultRow[KudosMessageTable.botMessageTs],
                        channelId = resultRow[KudosMessageTable.channelId],
                        createdAt = resultRow[KudosMessageTable.createdAt],
                        expiresAt = resultRow[KudosMessageTable.expiresAt],
                        userCounts = parseUserCounts(resultRow[KudosMessageTable.userCounts]),
                    )
                }
                .singleOrNull()
        }
    }

    private fun parseUserCounts(json: String): Map<String, Int> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            json.trim('{', '}')
                .split(",")
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val userId = parts[0].trim().trim('"')
                        val count = parts[1].trim().toIntOrNull()
                        if (count != null) userId to count else null
                    } else {
                        null
                    }
                }
                .toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeUserCounts(userCounts: Map<String, Int>): String {
        if (userCounts.isEmpty()) return "{}"
        return userCounts.entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
        ) { (userId, count) ->
            "\"$userId\":$count"
        }
    }

    suspend fun storeMessageWithWindow(
        threadTs: String,
        botMessageTs: String,
        channelId: String,
        userCounts: Map<String, Int> = emptyMap(),
        // 3 hours default window
        windowDurationMs: Long = 3 * 60 * 60 * 1000,
    ) {
        val now = System.currentTimeMillis()
        dbQuery {
            KudosMessageTable.insert {
                it[KudosMessageTable.threadTs] = threadTs
                it[KudosMessageTable.botMessageTs] = botMessageTs
                it[KudosMessageTable.channelId] = channelId
                it[createdAt] = now
                it[expiresAt] = now + windowDurationMs
                it[KudosMessageTable.userCounts] = serializeUserCounts(userCounts)
            }
        }
    }

    suspend fun updateMessageUserCounts(
        threadTs: String,
        botMessageTs: String,
        userCounts: Map<String, Int>,
    ) {
        dbQuery {
            KudosMessageTable.update(
                where = {
                    (KudosMessageTable.threadTs eq threadTs) and
                        (KudosMessageTable.botMessageTs eq botMessageTs)
                },
            ) {
                it[KudosMessageTable.userCounts] = serializeUserCounts(userCounts)
            }
        }
    }

    /**
     * Checks if a kudos transaction from giverId to recipientId would violate rate limits.
     * Returns null if allowed, or a String with the reason/time remaining if blocked.
     *
     * When spamProtectionEnabled = true:
     *   - Rule 1: One kudos per giver→recipient per thread
     *   - Rule 2: 5 minute global cooldown per giver→recipient pair
     *
     * When spamProtectionEnabled = false:
     *   - No restrictions - always returns null
     */
    suspend fun hasRecentKudos(
        giverId: String,
        recipientId: String,
        threadTs: String,
    ): String? {
        // Skip ALL checks if spam protection is disabled
        if (!spamProtectionEnabled) {
            return null
        }

        return dbQuery {
            val now = System.currentTimeMillis()
            val fiveMinutesAgo = now - (5 * 60 * 1000)

            // Rule 1: Per-thread deduplication (only when spam protection enabled)
            val threadTransaction =
                KudosTransactionTable
                    .select {
                        (KudosTransactionTable.giverId eq giverId) and
                            (KudosTransactionTable.recipientId eq recipientId) and
                            (KudosTransactionTable.threadTs eq threadTs)
                    }
                    .singleOrNull()

            if (threadTransaction != null) {
                return@dbQuery "You already gave kudos here! 😊"
            }

            // Rule 2: 5-minute cooldown check
            val recentTransaction =
                KudosTransactionTable
                    .select {
                        (KudosTransactionTable.giverId eq giverId) and
                            (KudosTransactionTable.recipientId eq recipientId) and
                            (KudosTransactionTable.timestamp greater fiveMinutesAgo)
                    }
                    .orderBy(KudosTransactionTable.timestamp, SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()

            if (recentTransaction != null) {
                val lastTime = recentTransaction[KudosTransactionTable.timestamp]
                val remainingMs = (lastTime + 5 * 60 * 1000) - now
                val remainingMin = (remainingMs / 1000 / 60).toInt()
                val remainingSec = ((remainingMs / 1000) % 60).toInt()

                return@dbQuery if (remainingMin > 0) {
                    "You already gave kudos recently! (try again in ${remainingMin}m ${remainingSec}s)"
                } else {
                    "You already gave kudos recently! (try again in ${remainingSec}s)"
                }
            }

            null
        }
    }

    /**
     * Records a kudos transaction for rate limiting purposes.
     */
    suspend fun recordTransaction(
        giverId: String,
        recipientId: String,
        threadTs: String,
    ) {
        try {
            dbQuery {
                KudosTransactionTable.insert {
                    it[KudosTransactionTable.giverId] = giverId
                    it[KudosTransactionTable.recipientId] = recipientId
                    it[KudosTransactionTable.threadTs] = threadTs
                    it[KudosTransactionTable.timestamp] = System.currentTimeMillis()
                }
            }
        } catch (e: Exception) {
            // Gracefully handle duplicate key errors when spam protection is disabled
            if (spamProtectionEnabled) {
                // If spam protection is enabled, this should never happen (hasRecentKudos prevents it)
                // Re-throw to surface the error
                throw e
            }
            // If spam protection is disabled, silently ignore duplicate key errors
            // This allows unlimited kudos on the same message
        }
    }
}
