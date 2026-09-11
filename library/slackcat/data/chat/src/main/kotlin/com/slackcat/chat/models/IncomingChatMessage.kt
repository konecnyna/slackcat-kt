package com.slackcat.chat.models

import com.slackcat.common.BotMessage

data class IncomingChatMessage(
    val arguments: List<String>,
    val command: String,
    val channelId: String,
    val chatUser: ChatUser,
    val messageId: String,
    val rawMessage: String,
    var threadId: String? = null,
    val userText: String,
    val attachments: List<ChatAttachment> = emptyList(),
)

data class ChatAttachment(
    val id: String,
    val name: String,
    val mimetype: String,
    val urlPrivate: String,
    val permalink: String,
) {
    val isImage: Boolean get() = mimetype.startsWith("image/")
}

data class OutgoingChatMessage(
    val channelId: String,
    val content: BotMessage,
    val threadId: String? = null,
    val plainText: Boolean = false,
)

sealed interface BotIcon {
    data class BotEmojiIcon(val emoji: String) : BotIcon

    data class BotImageIcon(val url: String) : BotIcon
}

data class ChatUser(val userId: String)

// Type aliases for better naming (will eventually replace the original names)
typealias UserMessage = IncomingChatMessage
typealias BotResponse = OutgoingChatMessage
