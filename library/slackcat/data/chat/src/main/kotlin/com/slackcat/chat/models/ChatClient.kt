package com.slackcat.chat.models

interface ChatClient {
    suspend fun sendMessage(message: OutgoingChatMessage): Result<Unit>
}
