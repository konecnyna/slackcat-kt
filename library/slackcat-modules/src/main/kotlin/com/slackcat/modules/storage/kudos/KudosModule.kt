package com.slackcat.modules.storage.kudos

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import org.jetbrains.exposed.sql.Table

open class KudosModule : SlackcatModule(), StorageModule {
    private val kudosDAO = KudosDAO()

    override fun tables(): List<Table> = listOf(KudosDAO.KudosTable)

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val allIds = extractUserIds(incomingChatMessage.userText)
        val ids = allIds.filter { it != incomingChatMessage.chatUser.userId }

        if (allIds.size == 1 && ids.isEmpty()) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    threadId = incomingChatMessage.messageId,
                    message = text("You'll go blind doing that!"),
                ),
            )
            return
        }

        ids.forEach {
            val updatedKudos = kudosDAO.upsertKudos(it)
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    threadId = incomingChatMessage.messageId,
                    message = text(getKudosMessage(updatedKudos)),
                ),
            )
        }
    }

    override fun provideCommand(): String = "++"

    override fun help(): String =
        buildMessage {
            title("KudosModule Help")
            text("Give kudos to your friends by using ?++ @username . See who can get the most!")
            text("You can also give kudos by reacting with :heavy_plus_sign: to their messages!")
        }

    override fun reactionsToHandle(): Set<String> = setOf("heavy_plus_sign")

    override suspend fun onReaction(event: SlackcatEvent) {
        when (event) {
            is SlackcatEvent.ReactionAdded -> {
                // Give kudos to the user who authored the message
                event.itemUserId?.let { messageAuthorId ->
                    // Don't allow self-kudos via reactions
                    if (messageAuthorId != event.userId) {
                        val updatedKudos = kudosDAO.upsertKudos(messageAuthorId)
                        sendMessage(
                            OutgoingChatMessage(
                                channelId = event.channelId,
                                threadId = event.messageTimestamp,
                                message = text(getKudosMessage(updatedKudos)),
                            ),
                        )
                    }
                }
            }

            is SlackcatEvent.ReactionRemoved -> {
                // Optionally handle kudos removal
                // For now, we'll keep kudos even if reactions are removed
            }

            else -> {
                // Ignore other events
            }
        }
    }

    private fun extractUserIds(userText: String): Set<String> {
        val pattern = """<@(\w+)>""".toRegex()
        return pattern.findAll(userText).map { it.groupValues[1] }.toSet()
    }

    protected open fun getKudosMessage(kudos: KudosDAO.KudosRow): String {
        return when (kudos.count) {
            1 -> "<@${kudos.userId}> now has ${kudos.count} plus"
            10 -> "<@${kudos.userId}> now has ${kudos.count} pluses! Double digits!"
            69 -> "Nice <@${kudos.userId}>"
            else -> {
                val plusText = if (kudos.count == 1) "plus" else "pluses"
                "<@${kudos.userId}> now has ${kudos.count} $plusText"
            }
        }
    }
}
