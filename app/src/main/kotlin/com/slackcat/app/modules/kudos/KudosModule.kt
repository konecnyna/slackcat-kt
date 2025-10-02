package com.slackcat.app.modules.kudos

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text

class KudosModule : SlackcatModule(), StorageModule {
    private val kudosDAO = KudosDAO()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val ids = extractUserIds(incomingChatMessage.userText)
        ids.forEach {
            val updatedKudos = kudosDAO.upsertKudos(it)
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    message = text("Bob now has $updatedKudos"),
                ),
            )
        }
    }

    override fun provideCommand(): String = "++"

    override fun help(): String = buildMessage {
        title("KudosModule Help")
        text("Give kudos to your friends by using ?++ @username . See who can get the most!")
    }

    override fun provideTables() = listOf(KudosDAO.KudosTable)

    private fun extractUserIds(userText: String): List<String> {
        val pattern = """<@(\w+)>""".toRegex()
        return pattern.findAll(userText).map { it.groupValues[1] }.toList()
    }
}
