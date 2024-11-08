package com.slackcat

import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.engine.cli.CliChatEngine
import com.slackcat.chat.engine.slack.SlackChatEngine
import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.database.DatabaseGraph
import com.slackcat.internal.Router
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import kotlinx.coroutines.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class SlackcatBot(
    val modules: Array<KClass<out SlackcatModule>>,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {
    lateinit var chatEngine: ChatEngine
    lateinit var chatClient: ChatClient
    lateinit var router: Router

    fun start(args: String?) {
        setupChatModule(args)
        connectDatabase()
        observeRealTimeMessages()
    }

    private fun setupChatModule(args: String?) {
        chatEngine =
            if (!args.isNullOrEmpty()) {
                CliChatEngine(args)
            } else {
                SlackChatEngine(coroutineScope)
            }

        chatClient =
            object : ChatClient {
                override fun sendMessage(message: OutgoingChatMessage) {
                    coroutineScope.launch { chatEngine.sendMessage(message) }
                }
            }

        val slackcatModules: List<SlackcatModule> =
            modules.map {
                it.createInstance().also { module -> module.chatClient = chatClient }
            }
        router = Router(slackcatModules)

        chatEngine.connect()
    }

    private fun connectDatabase() {
        val databaseFeatures: List<StorageModule> =
            modules
                .filter { it is StorageModule }
                .map { it as StorageModule }

        val exposedTables = databaseFeatures.map { it.provideTable() }
        DatabaseGraph.connectDatabase(exposedTables)
    }

    private fun observeRealTimeMessages() {
        runBlocking {
            println("Starting slackcat using ${chatEngine.provideEngineName()} engine")
            supervisorScope {
                chatEngine.eventFlow().collect { event ->
                    launch {
                        val handled = router.onMessage(event)
                        if (!handled && chatEngine is CliChatEngine) {
                            println("🚨 ${CliChatEngine.ERROR_MESSAGE}")
                        }
                    }
                }
            }
        }
    }
}
