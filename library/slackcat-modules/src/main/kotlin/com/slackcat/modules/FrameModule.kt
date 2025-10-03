package com.slackcat.modules

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage

class FrameModule : SlackcatModule() {


    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val inputUrl = incomingChatMessage.userText.replace("<", "").replace(">", "")
        val imageUrl = when (incomingChatMessage.command) {
            "nickelback" -> NICKELBACK_BASE + inputUrl
            FrameModuleAliases.Krang.alias -> KRANG_BASE + inputUrl
            else -> null
        }

        if (imageUrl == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = buildRichMessage {
                    image(
                        imageUrl = imageUrl,
                        altText = "frame image"
                    )
                }
            )
        )
    }

    override fun provideCommand(): String = "nickelback"
    override fun aliases(): List<String> = FrameModuleAliases.entries.map { it.alias }

    override fun help(): String = buildMessage {
        title("Frame Help")
        text("Put an image url into a fun frame [nickelback,krang]")
        text("Usage: ?nickelback https://ca.slack-edge.com/T07UUET6K51-U07UMV791SS-395a3cadb6fd-512")
    }


    private companion object {
        const val NICKELBACK_BASE = "https://home-remote-api.herokuapp.com/nickelback?url="
        const val KRANG_BASE = "https://home-remote-api.herokuapp.com/krang?url="
    }
}


enum class FrameModuleAliases(val alias: String) {
    Krang("krang")
}
