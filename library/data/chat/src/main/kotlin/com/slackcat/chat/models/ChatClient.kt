package com.slackcat.chat.models

interface ChatClient {
    fun sendMessage(message: OutgoingChatMessage)
}
