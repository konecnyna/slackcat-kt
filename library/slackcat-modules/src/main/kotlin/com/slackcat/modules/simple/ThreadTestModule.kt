package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.common.textMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule

class ThreadTestModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val topLevelResult =
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content =
                        buildMessage {
                            text("Thread test: top-level message")
                        },
                ),
            )

        topLevelResult.onSuccess { ts ->
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content = textMessage("Thread test: this reply should render as a text block in the thread"),
                    threadId = ts,
                ),
            )
        }
    }

    override fun commandInfo() =
        CommandInfo(
            command = "thread-test",
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("ThreadTest Help")
            text("Posts a top-level message, then a thread reply using text blocks.")
            text("Used to verify that text blocks render properly in threads.")
        }
}
