package com.slackcat.chat.models

interface ChatClient {
    suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<String>

    suspend fun updateMessage(
        channelId: String,
        messageTs: String,
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<String>

    suspend fun getUserDisplayName(userId: String): Result<String>

    suspend fun getMessageText(
        channelId: String,
        messageTs: String,
        threadTs: String? = null,
    ): Result<String> = Result.failure(UnsupportedOperationException("Not supported"))

    suspend fun getThreadRepliers(
        channelId: String,
        threadTs: String,
    ): Result<List<String>> = Result.failure(UnsupportedOperationException("Not supported"))

    suspend fun getUserGroupMembers(usergroupId: String): Result<List<String>> =
        Result.failure(UnsupportedOperationException("Not supported"))
}
