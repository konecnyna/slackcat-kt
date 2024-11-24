package com.slackcat.app.modules.deploybot

import com.slackcat.app.BigHipsChannels
import com.slackcat.app.Environment
import com.slackcat.app.SlackcatAppGraph.ENV
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatEventsModule
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text

class DeployBotModule : SlackcatModule(), SlackcatEventsModule {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // no - op
    }

    override fun provideCommand(): String = "deploy-bot"
    override fun help(): String = buildMessage {
        title("DeployBoyModule Help")
        text("Send a kewl message when slackcat starts")
    }

    override fun onEvent(event: SlackcatEvent) {
        if (ENV != Environment.Production) {
            return
        }

        val message = when (event) {
            SlackcatEvent.STARTED -> "I've started! MEOW!"
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = BigHipsChannels.SlackcatTesting.channelId,
                message = text(message)
            )
        )
    }
}
