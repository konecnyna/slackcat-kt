package com.slackcat.app.modules.bighips

import com.features.slackcat.models.SlackcatModule
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage

class BigHipsModule : SlackcatModule() {
    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val id = extractUserIds(incomingChatMessage.userText)
        println(incomingChatMessage)
        val text = """:alphabet-white-b::alphabet-white-i::alphabet-white-g:
            | :alphabet-white-h::alphabet-white-i::alphabet-white-p::alphabet-white-s:
        """.trimMargin()

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channeId,
                text = text,
            ),
        )
    }

    private fun extractUserIds(userText: String): List<String> {
        val pattern = """<@(\w+)>""".toRegex()
        return pattern.findAll(userText).map { it.groupValues[1] }.toList()
    }

    override fun provideCommand(): String = "big-hips"
}
