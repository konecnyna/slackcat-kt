package com.slackcat

import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.engine.cli.CliChatEngine
import com.slackcat.chat.engine.slack.SlackChatEngine
import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.database.DatabaseGraph
import com.slackcat.internal.Router
import com.slackcat.models.NetworkModule
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.network.NetworkClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.exposed.sql.Table
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class SlackcatBot(
    val modulesClasses: Array<KClass<out SlackcatModule>>,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    val databaseConfig: DataSource,
    val networkClient: NetworkClient? = null
) {
    lateinit var chatEngine: ChatEngine
    lateinit var chatClient: ChatClient
    lateinit var router: Router

    private val _events = MutableSharedFlow<SlackcatEvent>()
    private val eventsFlow = _events.asSharedFlow()


    fun start(args: String?) {
        val modules = setupChatModule(args)
        connectDatabase(modules, databaseConfig)
        observeRealTimeMessages()
    }

    private fun setupChatModule(args: String?): List<SlackcatModule> {
        chatEngine = if (!args.isNullOrEmpty()) {
            CliChatEngine(args, coroutineScope)
        } else {
            SlackChatEngine(coroutineScope)
        }

        chatClient = object : ChatClient {
            override suspend fun sendMessage(message: OutgoingChatMessage): Result<Unit> {
                return chatEngine.sendMessage(message)
            }
        }

        val slackcatModules: List<SlackcatModule> = modulesClasses.map {
            it.createInstance().also { module ->
                module.chatClient = chatClient
                module.coroutineScope = coroutineScope
                if (module is NetworkModule && networkClient != null) {
                    module.networkClient = networkClient
                }
            }
        }

        router = Router(
            modules = slackcatModules,
            coroutineScope = coroutineScope,
            eventsFlow = eventsFlow
        )

        chatEngine.connect { coroutineScope.launch { _events.emit(SlackcatEvent.STARTED) } }
        return slackcatModules
    }

    private fun connectDatabase(modules: List<SlackcatModule>, databaseConfig: DataSource) {
        val databaseFeatures: List<StorageModule> = modules
            .filter { it is StorageModule }
            .map { it as StorageModule }
        val exposedTables = databaseFeatures.map { it.provideTables() }.flatMap { it }
        println(exposedTables)
        DatabaseGraph.connectDatabase(exposedTables, databaseConfig)
    }

    private fun observeRealTimeMessages() {
        runBlocking {
            println("Starting slackcat using ${chatEngine.provideEngineName()} engine")
            supervisorScope {
                chatEngine.eventFlow().collect { event ->
                    launch {
                        val handled = router.onMessage(event)
                        if (!handled && chatEngine is CliChatEngine) {
                            println(CliChatEngine.ERROR_MESSAGE)
                        }
                    }
                }
            }
        }
    }
}
