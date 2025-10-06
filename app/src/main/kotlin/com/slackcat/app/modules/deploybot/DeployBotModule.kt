package com.slackcat.app.modules.deploybot

import com.slackcat.Engine
import com.slackcat.app.BigHipsChannels
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatEventsModule
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import org.koin.core.component.inject

class DeployBotModule : SlackcatModule(), SlackcatEventsModule {
    private val engine: Engine by inject()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // no - op
    }

    override fun provideCommand(): String = "deploy-bot"

    override fun help(): String =
        buildMessage {
            title("DeployBoyModule Help")
            text("Send a kewl message when slackcat starts")
        }

    override suspend fun onEvent(event: SlackcatEvent) {
        if (engine != Engine.ChatClient.Slack) {
            return
        }

        val message =
            when (event) {
                SlackcatEvent.STARTED -> "I've started! MEOW!"
                is SlackcatEvent.ReactionAdded, is SlackcatEvent.ReactionRemoved -> return
            }

        sendMessage(
            OutgoingChatMessage(
                channelId = BigHipsChannels.SlackcatTesting.channelId,
                message = text(message),
            ),
        )
    }
}
