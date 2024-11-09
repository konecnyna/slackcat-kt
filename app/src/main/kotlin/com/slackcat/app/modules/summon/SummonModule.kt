package com.slackcat.app.modules.summon

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage

class SummonModule : SlackcatModule() {
    private val summonClient = SummonClient()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        summonClient.getHtml()
    }

    override fun provideCommand(): String = "summon"

    override fun help(): String = buildMessage {
        title("Summon Help")
        text("Summon an image from Google images. Use with caution...")
        text("Usage: ?summon slackcat")
    }

}
