package features.bot

import data.chat.ChatGraph
import data.chat.engine.cli.CliChatEngine
import data.chat.engine.slack.SlackChatEngine
import data.chat.models.ChatClient
import data.chat.models.OutgoingChatMessage
import data.database.DatabaseGraph
import data.database.models.StorageClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SlackcatBot(
    val coroutineScope: CoroutineScope,
    val router: Router
) {

    fun start(args: String?) {
        setupChatModule(args)
        connectDatabase()
        observeRealTimeMessages()
    }

    private fun setupChatModule(args: String?) {
        ChatGraph.chatEngine =
            if (!args.isNullOrEmpty()) {
                CliChatEngine(args)
            } else {
                SlackChatEngine()
            }

        ChatGraph.chatClient = object : ChatClient {
            override fun sendMessage(message: OutgoingChatMessage) {
                coroutineScope.launch { ChatGraph.chatEngine.sendMessage(message) }
            }
        }

        ChatGraph.chatEngine.connect()
    }

    private fun connectDatabase() {
//        val databaseFeatures: List<StorageClient> =
//            FeatureGraph.features
//                .filter { it is StorageClient }
//                .map { it as StorageClient }
//
//        DatabaseGraph.connectDatabase(databaseFeatures)
    }

    private fun observeRealTimeMessages() {
        runBlocking {
            println("Starting slackcat using ${ChatGraph.chatEngine.provideEngineName()} engine")
            ChatGraph.chatEngine.eventFlow().collect {
                val handled = router.onMessage(it)
                if (!handled && ChatGraph.chatEngine is CliChatEngine) {
                    throw Error(CliChatEngine.commandNotHandledErrorMessage)
                }
            }
        }
    }
}