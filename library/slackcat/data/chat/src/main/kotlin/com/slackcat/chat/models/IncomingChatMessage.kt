package com.slackcat.chat.models

import com.slackcat.common.RichTextMessage
import com.slackcat.common.SlackcatAppDefaults.DEFAULT_BOT_IMAGE_ICON
import com.slackcat.common.SlackcatAppDefaults.DEFAULT_BOT_NAME

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
    val message: RichTextMessage = RichTextMessage(""),
    val threadId: String? = null,
)

sealed interface BotIcon {
    data class BotEmojiIcon(val emoji: String) : BotIcon

    data class BotImageIcon(val url: String) : BotIcon
}

data class ChatUser(val userId: String)
