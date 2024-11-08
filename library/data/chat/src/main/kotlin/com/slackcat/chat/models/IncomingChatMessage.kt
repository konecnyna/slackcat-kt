package com.slackcat.chat.models

data class IncomingChatMessage(
    val arguments: List<String>,
    val command: String,
    val channelId: String,
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    var threadId: String? = null,
    val userText: String
)

data class OutgoingChatMessage(
    val channelId: String,
    val text: String,
)

data class ChatUser(val userId: String)
