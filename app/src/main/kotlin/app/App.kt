package app

import app.AppGraph.globalScope
import app.common.Router
import chat.ChatGraph
import chat.models.ChatClient
import chat.engine.cli.CliChatEngine
import chat.engine.slack.SlackChatEngine
import chat.models.OutgoingChatMessage
import data.database.DatabaseGraph.connectDatabase
import data.database.models.StorageClient
import features.FeatureGraph
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class App {
    private val router = Router()

    fun onCreate(args: String?) {
        setupChatModule(args)
        connectDatabase()
        observeRealTimeMessages()
    }


    private fun setupChatModule(args: String?) {
        ChatGraph.chatEngine = if (!args.isNullOrEmpty()) {
            CliChatEngine(args)
        } else {
            SlackChatEngine()
        }

        ChatGraph.chatClient = object : ChatClient {
            override fun sendMessage(message: OutgoingChatMessage) {
                globalScope.launch { ChatGraph.chatEngine.sendMessage(message) }
            }
        }
    }

    private fun connectDatabase() {
        val databaseFeatures: List<StorageClient> = FeatureGraph.features
            .filter { it is StorageClient }
            .map { it as StorageClient }

        connectDatabase(databaseFeatures)
    }

    private fun observeRealTimeMessages() {
        runBlocking {
            println("Starting slackcat using ${ChatGraph.chatEngine.provideEngineName()} engine")
            ChatGraph.chatEngine.connect()
            ChatGraph.chatEngine.eventFlow().collect {
                val handled = router.onMessage(it)
                if (!handled && ChatGraph.chatEngine is CliChatEngine) {
                    throw Error(CliChatEngine.commandNotHandledErrorMessage)
                }
            }
        }
    }

}