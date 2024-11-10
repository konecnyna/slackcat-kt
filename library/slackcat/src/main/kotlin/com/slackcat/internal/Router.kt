package com.slackcat.internal

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import com.slackcat.common.SlackcatEvent
import com.slackcat.models.SlackcatEventsModule
import com.slackcat.models.SlackcatModule
import com.slackcat.models.UnhandledCommandModule
import com.slackcat.presentation.buildMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Router(
    private val modules: List<SlackcatModule>,
    private val coroutineScope: CoroutineScope,
    private val eventsFlow: SharedFlow<SlackcatEvent>
) {
    private val featureCommandMap: Map<String, SlackcatModule> by lazy {
        buildMap {
            modules.forEach { module ->
                try {
                    put(module.provideCommand(), module)
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }

    private val aliasCommandMap: Map<String, SlackcatModule> by lazy {
        buildMap {
            modules.forEach { module ->
                try {
                    module.aliases().forEach { alias ->
                        put(alias, module)
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }
    }


    private val eventModules: List<SlackcatEventsModule> by lazy {
        featureCommandMap
            .toList()
            .filter { it.second is SlackcatEventsModule }
            .map { it.second as SlackcatEventsModule }
    }

    private val unhandledCommandModuleModules: List<UnhandledCommandModule>
        get() = modules.filter { it is UnhandledCommandModule }
            .map { it as UnhandledCommandModule }


    init {
        subscribeToEvents()
    }


    /**
     * true -> message was handled by module
     * false -> message was NOT handled module
     */
    suspend fun onMessage(incomingMessage: IncomingChatMessage): Boolean {
        val command = CommandParser.extractCommand(incomingMessage.rawMessage)
        if (!CommandParser.validateCommandMessage(incomingMessage.rawMessage) || command == null) {
            return false
        }

        val feature = featureCommandMap[command] // Primary commands take precedence
            ?: aliasCommandMap[command] // Check alias modules next

        if (feature == null) {
            var handled = false
            unhandledCommandModuleModules.forEach {
                if (it.onUnhandledCommand(incomingMessage)) {
                    handled = true
                }
            }
            return handled
        }

        return try {
            when {
                incomingMessage.arguments.contains("--help") -> {
                    val helpMessage =
                        OutgoingChatMessage(
                            channelId = incomingMessage.channelId,
                            text = feature.help(),
                        )
                    feature.sendMessage(helpMessage)
                }

                else -> withContext(Dispatchers.IO) {
                    feature.onInvoke(incomingMessage)
                }
            }
            true
        } catch (exception: Exception) {
            handleError(feature, incomingMessage, exception)
            false
        }
    }

    private fun handleError(
        feature: SlackcatModule,
        incomingMessage: IncomingChatMessage,
        exception: Exception,
    ) {
        val errorMessage =
            buildMessage {
                title("🚨 Error")
                text("The ${feature::class.java.canonicalName} module encountered an error!")
                text("Error: '${exception.message}'")
            }
        feature.sendMessage(
            OutgoingChatMessage(
                channelId = incomingMessage.channelId,
                text = errorMessage,
            ),
        )
    }

    private fun subscribeToEvents() = coroutineScope.launch {
        eventsFlow.collect { event ->
            eventModules.forEach { module -> module.onEvent(event) }
        }
    }
}
