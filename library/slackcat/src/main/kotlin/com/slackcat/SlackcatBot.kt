package com.slackcat

import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.engine.cli.CliChatEngine
import com.slackcat.chat.engine.slack.SlackChatEngine
import com.slackcat.chat.models.ChatClient
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.SlackcatEvent
import com.slackcat.database.DatabaseGraph
import com.slackcat.internal.Router
import com.slackcat.models.SlackcatModule
import com.slackcat.models.StorageModule
import com.slackcat.network.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class SlackcatBot(
    val modulesClasses: Array<KClass<out SlackcatModule>>,
    val databaseConfig: DataSource,
    val networkClient: NetworkClient? = null,
) : KoinComponent {
    lateinit var chatEngine: ChatEngine
    lateinit var chatClient: ChatClient
    lateinit var router: Router

    private val coroutineScope: CoroutineScope = get()

    private val events = MutableSharedFlow<SlackcatEvent>()
    val eventsFlow = events.asSharedFlow()

    fun start(args: String?) {
        val modules = setupChatModule(args)
        connectDatabase(modules, databaseConfig)
        observeRealTimeMessages()
    }

    private fun setupChatModule(args: String?): List<SlackcatModule> {
        chatEngine =
            if (!args.isNullOrEmpty()) {
                CliChatEngine(args, coroutineScope)
            } else {
                SlackChatEngine(coroutineScope)
            }

        // Wire the events flow to the chat engine so it can emit reaction events
        chatEngine.setEventsFlow(events)

        chatClient =
            object : ChatClient {
                override suspend fun sendMessage(
                    message: OutgoingChatMessage,
                    botName: String,
                    botIcon: com.slackcat.chat.models.BotIcon,
                ): Result<Unit> {
                    return chatEngine.sendMessage(message, botName, botIcon)
                }

                override suspend fun getUserDisplayName(userId: String): Result<String> {
                    return (chatEngine as? SlackChatEngine)?.getUserDisplayName(userId)
                        ?: Result.failure(Exception("getUserDisplayName not supported by this chat engine"))
                }
            }

        // Register ChatClient with Koin
        getKoin().declare(chatClient)

        // First pass: instantiate modules without Router dependency
        val modulesWithoutRouter: List<SlackcatModule> =
            modulesClasses
                .filter { moduleClass ->
                    // Filter out modules that require Router
                    !moduleClass.constructors.any {
                        it.parameters.size == 1 &&
                            it.parameters[0].type.classifier == Router::class
                    }
                }
                .map { moduleClass ->
                    val module =
                        try {
                            // Try NetworkClient constructor
                            if (networkClient != null && moduleClass.constructors.any { it.parameters.size == 1 }) {
                                moduleClass.constructors
                                    .firstOrNull { it.parameters.size == 1 }
                                    ?.call(networkClient)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            null
                        } ?: moduleClass.createInstance() // Fallback to no-arg constructor

                    module
                }

        // Create temporary router for modules that need Router in constructor
        // Note: This router won't subscribe to events yet (will be replaced before events are emitted)
        router =
            Router(
                modules = modulesWithoutRouter,
                coroutineScope = coroutineScope,
                eventsFlow = eventsFlow,
            )

        // Second pass: instantiate modules that require Router
        val modulesWithRouter: List<SlackcatModule> =
            modulesClasses
                .filter { moduleClass ->
                    // Find modules that require Router
                    moduleClass.constructors.any {
                        it.parameters.size == 1 &&
                            it.parameters[0].type.classifier == Router::class
                    }
                }
                .map { moduleClass ->
                    val module =
                        moduleClass.constructors
                            .firstOrNull {
                                it.parameters.size == 1 &&
                                    it.parameters[0].type.classifier == Router::class
                            }
                            ?.call(router)
                            ?: moduleClass.createInstance()

                    module
                }

        // Combine all modules
        val slackcatModules = modulesWithoutRouter + modulesWithRouter

        // Cancel the temporary router's event subscription before creating the final router
        router.cancelEventsSubscription()

        // Replace router with final version containing all modules
        router =
            Router(
                modules = slackcatModules,
                coroutineScope = coroutineScope,
                eventsFlow = eventsFlow,
            )

        chatEngine.connect { coroutineScope.launch { events.emit(SlackcatEvent.STARTED) } }
        return slackcatModules
    }

    private fun connectDatabase(
        modules: List<SlackcatModule>,
        databaseConfig: DataSource,
    ) {
        val databaseFeatures: List<StorageModule> =
            modules
                .filter { it is StorageModule }
                .map { it as StorageModule }
        val exposedTables = databaseFeatures.map { it.tables() }.flatMap { it }
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
