package app.common

import app.AppGraph
import data.chat.ChatGraph
import data.chat.engine.cli.CliChatEngine
import data.chat.engine.slack.SlackChatEngine
import data.chat.models.ChatClient
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import data.database.DatabaseGraph
import data.database.models.StorageClient
import features.FeatureGraph
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SlackcatBot {
    fun start(args: String?, onMessage: (IncomingChatMessage) -> Unit) {
        setupChatModule(args)
        connectDatabase()
        observeRealTimeMessages(onMessage)
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
                AppGraph.globalScope.launch { ChatGraph.chatEngine.sendMessage(message) }
            }
        }

        ChatGraph.chatEngine.connect()
    }

    private fun connectDatabase() {
        val databaseFeatures: List<StorageClient> =
            FeatureGraph.features
                .filter { it is StorageClient }
                .map { it as StorageClient }

        DatabaseGraph.connectDatabase(databaseFeatures)
    }

    private fun observeRealTimeMessages(onMessage: (IncomingChatMessage) -> Unit) {
        runBlocking {
            println("Starting slackcat using ${ChatGraph.chatEngine.provideEngineName()} engine")
            ChatGraph.chatEngine.eventFlow().collect { onMessage(it) }
        }
    }
}