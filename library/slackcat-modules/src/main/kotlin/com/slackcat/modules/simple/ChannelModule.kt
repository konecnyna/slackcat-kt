package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule

class ChannelModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("Channel ID: ${incomingChatMessage.channelId}"),
                threadId = incomingChatMessage.messageId,
            ),
        )
    }

    override fun commandInfo() =
        CommandInfo(
            command = "channel",
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("Channel Help")
            text("Prints the current channel ID in a thread.")
        }
}
