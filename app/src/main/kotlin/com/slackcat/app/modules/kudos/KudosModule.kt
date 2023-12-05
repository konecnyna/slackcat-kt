package com.slackcat.app.modules.kudos

import com.features.slackcat.models.SlackcatModule
import com.features.slackcat.models.StorageModule
import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.launch

class KudosModule : SlackcatModule(), StorageModule {
    val kudosDAO = KudosDAO()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        globalScope.launch {
            val ids = extractUserIds(incomingChatMessage.userText)
            ids.forEach {
                val updatedKudos = kudosDAO.upsertKudos(it)
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channeId,
                        text = "Bob now has $updatedKudos",
                    ),
                )
            }
        }
    }

    override fun provideCommand(): String = "++"

    override fun provideTable() = KudosDAO.KudosTable

    private fun extractUserIds(userText: String): List<String> {
        val pattern = """<@(\w+)>""".toRegex()
        return pattern.findAll(userText).map { it.groupValues[1] }.toList()
    }
}
