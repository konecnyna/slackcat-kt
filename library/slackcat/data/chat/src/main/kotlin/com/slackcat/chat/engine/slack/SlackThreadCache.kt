package com.slackcat.chat.engine.slack

import com.slack.api.methods.MethodsClient

class SlackThreadCache(private val client: MethodsClient) {
    private data class ThreadCacheEntry(val threadTs: String?, val timestamp: Long)

    private val threadCache = mutableMapOf<String, ThreadCacheEntry>()
    private val cacheTtlMs = 24 * 60 * 60 * 1000L // 24 hours

    fun cacheThreadMapping(
        messageTs: String,
        threadTs: String?,
    ) {
        val now = System.currentTimeMillis()
        threadCache[messageTs] = ThreadCacheEntry(threadTs, now)

        // Cleanup expired entries (older than TTL)
        threadCache.entries.removeIf { (_, entry) ->
            now - entry.timestamp > cacheTtlMs
        }
    }

    suspend fun resolveThreadRoot(
        channelId: String,
        messageTs: String,
    ): String? {
        // Check cache first
        val cached = threadCache[messageTs]
        if (cached != null) {
            return cached.threadTs
        }

        // Cache miss - fetch from API
        return try {
            val response =
                client.conversationsHistory { req ->
                    req.channel(channelId)
                        .latest(messageTs)
                        .inclusive(true)
                        .limit(1)
                }

            if (response.isOk && response.messages.isNotEmpty()) {
                val message = response.messages[0]
                val threadTs = message.threadTs

                // Cache the result for future lookups
                cacheThreadMapping(messageTs, threadTs)

                threadTs
            } else {
                null
            }
        } catch (e: Exception) {
            println("[SlackChatEngine] Failed to fetch thread info for $messageTs: ${e.message}")
            null
        }
    }
}
