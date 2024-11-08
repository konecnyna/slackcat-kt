package com.slackcat.chat.models

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

data class IncomingChatMessage(
    val arguments: List<String>,
    val command: String,
    val channelId: String,
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    var threadId: String? = null,
    val userText: String,
)

data class OutgoingChatMessage(
    val channelId: String,
    val text: String,
    val blocks: JsonObject = buildJsonObject { },
    val userName: String? = "slackcat",
    val iconUrl: String? = null,
)

data class ChatUser(val userId: String)
