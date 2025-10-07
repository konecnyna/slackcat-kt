package com.slackcat.presentation

import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.MessageBuilder
import com.slackcat.common.MessageStyle
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage

/**
 * Create an outgoing message with the new platform-agnostic format.
 */
fun newMessage(
    channelId: String,
    message: BotMessage,
    threadId: String? = null,
): OutgoingChatMessage {
    return OutgoingChatMessage(
        channelId = channelId,
        newMessage = message,
        threadId = threadId,
    )
}

/**
 * Create an outgoing message using the new builder DSL.
 */
fun newMessage(
    channelId: String,
    style: MessageStyle? = null,
    threadId: String? = null,
    block: MessageBuilder.() -> Unit,
): OutgoingChatMessage {
    return OutgoingChatMessage(
        channelId = channelId,
        newMessage = buildMessage(style, block),
        threadId = threadId,
    )
}

/**
 * Create a simple text message using the new format.
 */
fun newTextMessage(
    channelId: String,
    text: String,
    threadId: String? = null,
): OutgoingChatMessage {
    return OutgoingChatMessage(
        channelId = channelId,
        newMessage = textMessage(text),
        threadId = threadId,
    )
}
