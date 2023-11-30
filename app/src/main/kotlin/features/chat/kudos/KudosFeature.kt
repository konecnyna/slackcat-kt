package features.chat.kudos

import features.FeatureGraph.featureCoroutineScope
import features.common.StorageClient
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Table
import app.AppGraph.chatClient
import app.models.Message
import features.common.FeatureModule

class KudosFeature : FeatureModule(), StorageClient {
    val kudosDAO = KudosDAO()

    override fun onInvoke(message: Message) {
        featureCoroutineScope.launch {
            val updatedKudos = kudosDAO.upsertKudos(message.chatUser.userId)
            chatClient.sendMessage("Bob now has ${updatedKudos}")
        }

    }
    override fun provideCommand(): String = "++"
    override fun provideTable(): Table = KudosDAO.KudosTable
}


