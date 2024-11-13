package com.slackcat.chat.models

import com.slack.api.model.block.RichTextBlock
import com.slackcat.common.RichTextMessage
import com.slackcat.common.SlackcatAppDefaults.DEFAULT_BOT_IMAGE_ICON
import com.slackcat.common.SlackcatAppDefaults.DEFAULT_BOT_NAME
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
    val text: String = "",
    val richText: RichTextMessage = RichTextMessage(""),
    val botName: String = DEFAULT_BOT_NAME,
    val botIcon: BotIcon = BotIcon.BotImageIcon(DEFAULT_BOT_IMAGE_ICON),
)

fun RichTextMessage.outgoingMessage(chanelId: String) = OutgoingChatMessage(
    channelId = chanelId,
    richText = this
)

sealed interface BotIcon {
    data class BotEmojiIcon(val emoji: String) : BotIcon

    data class BotImageIcon(val url: String) : BotIcon
}

data class ChatUser(val userId: String)
