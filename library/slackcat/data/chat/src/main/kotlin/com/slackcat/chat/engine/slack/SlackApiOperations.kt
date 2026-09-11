package com.slackcat.chat.engine.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.model.File

class SlackApiOperations(private val client: MethodsClient) {
    /**
     * Shares the file publicly and returns a URL that renders in an image block.
     * Slack refuses `url_private` without a bearer token, so the `pub_secret` query is required.
     */
    suspend fun getPublicFileUrl(fileId: String): Result<String> {
        val file = shareFilePublicly(fileId).getOrElse { return Result.failure(it) }
        val urlPrivate =
            file.urlPrivate
                ?: return Result.failure(Exception("Slack returned no url_private for $fileId"))
        val publicSecret =
            file.permalinkPublic?.substringAfterLast('-')?.takeIf { it.isNotBlank() }
                ?: return Result.failure(Exception("Slack returned no permalink_public for $fileId"))
        return Result.success("$urlPrivate?pub_secret=$publicSecret")
    }

    private fun shareFilePublicly(fileId: String): Result<File> {
        return try {
            val response = client.filesSharedPublicURL { req -> req.file(fileId) }
            when {
                response.isOk && response.file != null -> Result.success(response.file)
                response.error == "already_public" -> readFile(fileId)
                else -> Result.failure(Exception("Slack API error: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readFile(fileId: String): Result<File> {
        return try {
            val response = client.filesInfo { req -> req.file(fileId) }
            if (response.isOk && response.file != null) {
                Result.success(response.file)
            } else {
                Result.failure(Exception("Slack API error: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessageText(
        channelId: String,
        messageTs: String,
        threadTs: String?,
    ): Result<String> {
        return try {
            if (threadTs != null) {
                val response =
                    client.conversationsReplies { req ->
                        req.channel(channelId)
                            .ts(threadTs)
                            .latest(messageTs)
                            .inclusive(true)
                            .limit(1)
                    }
                if (response.isOk && response.messages.isNotEmpty()) {
                    val message =
                        response.messages.find { it.ts == messageTs }
                            ?: response.messages[0]
                    Result.success(message.text ?: "")
                } else {
                    Result.failure(Exception("Failed to fetch message: ${response.error}"))
                }
            } else {
                val response =
                    client.conversationsHistory { req ->
                        req.channel(channelId)
                            .latest(messageTs)
                            .inclusive(true)
                            .limit(1)
                    }
                if (response.isOk && response.messages.isNotEmpty()) {
                    val message =
                        response.messages.find { it.ts == messageTs }
                            ?: response.messages[0]
                    Result.success(message.text ?: "")
                } else {
                    Result.failure(Exception("Failed to fetch message: ${response.error}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reads the files attached to a message. The Events API omits `files` for a hosted file that
     * Slack renders inline in a rich_text block, so the event alone cannot supply them.
     */
    suspend fun getMessageFiles(
        channelId: String,
        messageTs: String,
        threadTs: String?,
    ): Result<List<File>> {
        return try {
            val messages =
                if (threadTs != null) {
                    val response =
                        client.conversationsReplies { req ->
                            req.channel(channelId).ts(threadTs).latest(messageTs).inclusive(true).limit(1)
                        }
                    if (!response.isOk) return Result.failure(Exception("Slack API error: ${response.error}"))
                    response.messages
                } else {
                    val response =
                        client.conversationsHistory { req ->
                            req.channel(channelId).latest(messageTs).inclusive(true).limit(1)
                        }
                    if (!response.isOk) return Result.failure(Exception("Slack API error: ${response.error}"))
                    response.messages
                }
            val message = messages?.find { it.ts == messageTs } ?: messages?.firstOrNull()
            Result.success(message?.files.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserDisplayName(userId: String): Result<String> {
        return try {
            val response =
                client.usersInfo { req ->
                    req.user(userId)
                }

            if (response.isOk && response.user != null) {
                val displayName =
                    response.user.profile?.displayName?.takeIf { it.isNotBlank() }
                        ?: response.user.realName
                        ?: response.user.name
                        ?: userId
                Result.success(displayName)
            } else {
                Result.failure(Exception("Failed to fetch user info: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getThreadRepliers(
        channelId: String,
        threadTs: String,
    ): Result<List<String>> {
        return try {
            val userIds = mutableSetOf<String>()
            var cursor: String? = null

            do {
                val response =
                    client.conversationsReplies { req ->
                        req.channel(channelId)
                            .ts(threadTs)
                            .limit(200)
                        cursor?.let { req.cursor(it) }
                    }

                if (!response.isOk) {
                    return Result.failure(Exception("Slack API error: ${response.error}"))
                }

                response.messages
                    ?.filter { it.ts != threadTs }
                    ?.mapNotNull { it.user }
                    ?.let { userIds.addAll(it) }

                cursor = response.responseMetadata?.nextCursor?.takeIf { it.isNotEmpty() }
            } while (cursor != null)

            Result.success(userIds.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserGroupMembers(usergroupId: String): Result<List<String>> {
        return try {
            val response =
                client.usergroupsUsersList { req ->
                    req.usergroup(usergroupId)
                }

            if (response.isOk) {
                Result.success(response.users ?: emptyList())
            } else {
                Result.failure(Exception("Slack API error: ${response.error}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
