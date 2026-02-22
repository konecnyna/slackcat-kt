package com.slackcat.presentation

import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.MessageBuilder
import com.slackcat.common.MessageStyle
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage

/**
 * Create an outgoing message with platform-agnostic format.
 */
fun message(
    channelId: String,
    content: BotMessage,
    threadId: String? = null,
): OutgoingChatMessage {
    return OutgoingChatMessage(
        channelId = channelId,
        content = content,
        threadId = threadId,
    )
}

/**
 * Create an outgoing message using the builder DSL.
 */
fun message(
    channelId: String,
    style: MessageStyle? = null,
    threadId: String? = null,
    block: MessageBuilder.() -> Unit,
): OutgoingChatMessage {
    return OutgoingChatMessage(
        channelId = channelId,
        content = buildMessage(style, block),
        threadId = threadId,
    )
}

/**
 * Create a simple text message.
 */
fun textMessage(
    channelId: String,
    text: String,
    threadId: String? = null,
): OutgoingChatMessage {
    return OutgoingChatMessage(
        channelId = channelId,
        content = textMessage(text),
        threadId = threadId,
    )
}
