package com.slackcat.chat.engine.cli

import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.CommandParser
import com.slackcat.common.SlackcatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant

class CliChatEngine(
    private val args: String,
    scope: CoroutineScope,
) : ChatEngine {
    companion object {
        const val DIVIDER = "-----------------------------"
        const val ERROR_MESSAGE =
            "\n${DIVIDER}\n 🚨The incoming message wasn't handled.\n" +
                "* Please check to make sure its in the proper format. E.g. '?ping'\n" +
                "* Make sure to add your feature to 'FeatureGraph.kt'\n" +
                "* Make sure you added it to  modules list - " +
                "val modules: Array<KClass<out SlackcatModule>> = arrayOf(DateModule::class,...)\n" +
                DIVIDER
    }

    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    private var eventsFlow: MutableSharedFlow<SlackcatEvent>? = null

    init {
        scope.launch {
            delay(Duration.ofSeconds(1))

            // Check if this is a mock reaction command (e.g., "?react :thumbsup: 1234567890.123456")
            if (args.startsWith("?react ")) {
                handleMockReaction(args)
                return@launch
            }

            val command =
                CommandParser.extractCommand(args)
                    ?: throw IllegalArgumentException("No valid command given. Commands should be prefixed with ?")

            println("Incoming message: $args")
            val incomingMessage =
                IncomingChatMessage(
                    command = command,
                    channelId = "123456789",
                    chatUser = CliMockData.defaultCliUser,
                    messageId = Instant.now().toString(),
                    rawMessage = args,
                    arguments = CommandParser.extractArguments(args),
                    userText = CommandParser.extractUserText(args),
                )

            println("Emitting: $incomingMessage")
            _messagesFlow.emit(incomingMessage)
        }
    }

    private suspend fun handleMockReaction(args: String) {
        // Parse reaction command: ?react :emoji: [messageTs] [--remove]
        val parts = args.removePrefix("?react ").trim().split(" ")
        if (parts.isEmpty()) {
            println("Invalid reaction command. Usage: ?react :emoji: [messageTs] [--remove]")
            return
        }

        val emoji = parts[0].removeSurrounding(":")
        val messageTs = parts.getOrNull(1) ?: "1234567890.123456"
        val isRemove = parts.contains("--remove")

        val event =
            if (isRemove) {
                SlackcatEvent.ReactionRemoved(
                    userId = CliMockData.defaultCliUser.userId,
                    reaction = emoji,
                    channelId = "123456789",
                    messageTimestamp = messageTs,
                    itemUserId = "U987654321",
                    eventTimestamp = Instant.now().epochSecond.toString(),
                )
            } else {
                SlackcatEvent.ReactionAdded(
                    userId = CliMockData.defaultCliUser.userId,
                    reaction = emoji,
                    channelId = "123456789",
                    messageTimestamp = messageTs,
                    itemUserId = "U987654321",
                    eventTimestamp = Instant.now().epochSecond.toString(),
                )
            }

        println("Emitting mock reaction: $event")
        eventsFlow?.emit(event)
    }

    override fun connect(ready: () -> Unit) {
        println("${provideEngineName()} is connected")
        ready()
    }

    override suspend fun sendMessage(
        message: OutgoingChatMessage,
        botName: String,
        botIcon: BotIcon,
    ): Result<Unit> {
        return try {
            println("--------------------------------------")
            println("Outgoing message: channelId=${message.channelId}, botName=$botName, botIcon=$botIcon")
            println("User sees rich text:\n${message.message.text}")
            println("--------------------------------------")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun eventFlow(): SharedFlow<IncomingChatMessage> = messagesFlow

    override fun provideEngineName(): String = "Cli"

    override fun setEventsFlow(eventsFlow: MutableSharedFlow<SlackcatEvent>) {
        this.eventsFlow = eventsFlow
    }
}
