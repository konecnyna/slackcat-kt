package com.slackcat.app.modules.bighips

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text

class BigHipsModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val text =
            """:alphabet-white-b::alphabet-white-i::alphabet-white-g:
            | :alphabet-white-h::alphabet-white-i::alphabet-white-p::alphabet-white-s:
            """.trimMargin()

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(text),
            ),
        )
    }

    override fun provideCommand(): String = "big-hips"

    override fun help(): String = buildMessage {
        title("Big Hips Help")
        text("This module is for tommy big hips only! If you have regular hips please don't use.")
    }
}
