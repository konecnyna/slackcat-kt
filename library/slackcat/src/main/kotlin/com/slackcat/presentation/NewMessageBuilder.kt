package com.slackcat.presentation

import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.MessageBuilder
import com.slackcat.common.MessageStyle
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage

/**
 * Create an outgoing channel message with platform-agnostic format.
 */
fun message(
    channelId: String,
    content: BotMessage,
): OutgoingChatMessage.ChannelMessage {
    return OutgoingChatMessage.ChannelMessage(
        channelId = channelId,
        content = content,
    )
}

/**
 * Create an outgoing channel message using the builder DSL.
 */
fun message(
    channelId: String,
    style: MessageStyle? = null,
    block: MessageBuilder.() -> Unit,
): OutgoingChatMessage.ChannelMessage {
    return OutgoingChatMessage.ChannelMessage(
        channelId = channelId,
        content = buildMessage(style, block),
    )
}

/**
 * Create a simple text channel message.
 */
fun textMessage(
    channelId: String,
    text: String,
): OutgoingChatMessage.ChannelMessage {
    return OutgoingChatMessage.ChannelMessage(
        channelId = channelId,
        content = textMessage(text),
    )
}

/**
 * Create a thread reply with plain text only.
 */
fun threadReply(
    channelId: String,
    threadId: String,
    text: String,
): OutgoingChatMessage.ThreadReply {
    return OutgoingChatMessage.ThreadReply(
        channelId = channelId,
        threadId = threadId,
        text = text,
    )
}
