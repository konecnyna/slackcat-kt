package app.modules.kudos

import app.AppGraph.globalScope
import data.chat.models.IncomingChatMessage
import features.slackcat.models.SlackcatModule
import features.slackcat.models.StorageModule
import features.slackcat.models.TableEntity
import kotlinx.coroutines.launch

class KudosModule : SlackcatModule(), StorageModule {
    val kudosDAO = KudosDAO()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        globalScope.launch {
//            val updatedKudos = kudosDAO.upsertKudos(incomingChatMessage.chatUser.userId)
//            chatClient.sendMessage(
//                OutgoingChatMessage(
//                    channelId = incomingChatMessage.channeId,
//                    text = "Bob now has $updatedKudos"
//                ),
//            )
        }
    }

    override fun provideCommand(): String = "++"

    override fun provideTable() = KudosDAO.KudosTable
}
