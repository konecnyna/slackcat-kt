package com.slackcat.app.modules.deploybot

import com.slackcat.app.BigHipsChannels
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatEventsModule
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import org.jetbrains.exposed.sql.Table

class DeployBotModule : SlackcatModule(), SlackcatEventsModule {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // no - op
    }

    override fun provideCommand(): String = "big-hips"
    override fun help(): String = buildMessage {
        title("Big Hips Help")
        text("This module is for tommy big hips only! If you have regular hips please don't use.")
    }

    override fun onEvent(event: SlackcatEvent) {
        val message = when (event) {
            SlackcatEvent.STARTED -> "I've started! MEOW!"
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = BigHipsChannels.SlackcatTesting.channelId,
                text = message
            )
        )
    }
}
