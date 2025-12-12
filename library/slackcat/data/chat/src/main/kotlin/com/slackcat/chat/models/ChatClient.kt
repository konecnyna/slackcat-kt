package com.slackcat.chat.models

interface ChatClient {
    suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<Unit>

    suspend fun getUserDisplayName(userId: String): Result<String>
}
