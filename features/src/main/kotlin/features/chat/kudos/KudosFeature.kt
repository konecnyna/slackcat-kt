package features.chat.kudos

import data.chat.ChatGraph.chatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import data.database.models.StorageClient
import features.FeatureGraph.featureCoroutineScope
import features.common.FeatureModule
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Table

class KudosFeature : FeatureModule(), StorageClient {
    val kudosDAO = KudosDAO()

    override fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        featureCoroutineScope.launch {
            val updatedKudos = kudosDAO.upsertKudos(incomingChatMessage.chatUser.userId)
            chatClient.sendMessage(
                OutgoingChatMessage("Bob now has $updatedKudos"),
            )
        }
    }

    override fun provideCommand(): String = "++"

    override fun provideTable(): Table = KudosDAO.KudosTable
}
