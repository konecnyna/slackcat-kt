package features.chat.kudos

import chat.ChatGraph.chatClient
import chat.models.IncomingChatMessage
import chat.models.OutgoingChatMessage
import features.FeatureGraph.featureCoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Table
import data.database.models.StorageClient
import features.common.FeatureModule

class KudosFeature : FeatureModule(), StorageClient {
    val kudosDAO = KudosDAO()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        featureCoroutineScope.launch {
            val updatedKudos = kudosDAO.upsertKudos(incomingChatMessage.chatUser.userId)
            chatClient.sendMessage(
                OutgoingChatMessage("Bob now has $updatedKudos")
            )
        }

    }

    override fun provideCommand(): String = "++"
    override fun provideTable(): Table = KudosDAO.KudosTable
}


