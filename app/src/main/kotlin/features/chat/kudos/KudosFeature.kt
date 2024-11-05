package features.chat.kudos

import features.FeatureGraph.featureCoroutineScope
import features.common.StorageClient
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Table
import app.AppGraph.chatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import features.common.FeatureModule

class KudosFeature : FeatureModule(), StorageClient {
    val kudosDAO = KudosDAO()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        featureCoroutineScope.launch {
            val updatedKudos = kudosDAO.upsertKudos(incomingChatMessage.chatUser.userId)
            chatClient.sendMessage(
                OutgoingChatMessage(
                    channel = incomingChatMessage.channelId,
                    text  = "Bob now has ${updatedKudos}"
                )
            )
        }

    }

    override fun provideCommand(): String = "++"
    override fun provideTable(): Table = KudosDAO.KudosTable
}


