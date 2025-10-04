package com.slackcat.modules.storage.learn

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.presentation.text

class LearnAliasHandler(private val learnDAO: LearnDAO) {
    suspend fun handleAliases(incomingChatMessage: IncomingChatMessage): OutgoingChatMessage? {
        val alias =
            LearnAliases.fromAlias(incomingChatMessage.command)
                ?: return null

        val message =
            when (alias) {
                LearnAliases.Unlearn -> handleUnlearn(incomingChatMessage)
                LearnAliases.List -> handleList(incomingChatMessage.userText)
            }

        return OutgoingChatMessage(
            channelId = incomingChatMessage.channelId,
            message = text(message),
        )
    }

    private suspend fun handleUnlearn(message: IncomingChatMessage): String {
        val learnKey = message.userText.substringBefore(" --index").trim()
        val index = extractIndexFromText(message.userText)

        return if (index != null && learnDAO.removeEntryByIndex(learnKey, index - 1)) {
            "🎉 Poof! Entry #$index for \"$learnKey\" has vanished into the digital ether! 🌌"
        } else {
            "Oops! 😅 Couldn't find entry #$index for \"$learnKey\". Are you sure it exists?"
        }
    }

    private fun extractIndexFromText(userText: String): Int? {
        val regex = Regex("--index\\s+(\\d+)")
        return regex.find(userText)?.groupValues?.get(1)?.toIntOrNull()
    }

    private suspend fun handleList(learnKey: String): String {
        val entries = learnDAO.getEntriesByLearnKey(learnKey)
        return if (entries.isEmpty()) {
            "I couldn't find any entries for that $learnKey"
        } else {
            entries.mapIndexed { index, entry ->
                "${index + 1}. ${entry.learnText}"
            }.joinToString("\n")
        }
    }
}
