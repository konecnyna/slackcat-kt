package com.slackcat.chat.engine.cli

import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant

class CliChatEngine(private val args: String, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) : ChatEngine {
    companion object {
        const val ERROR_MESSAGE = "\n\nThe incoming message wasn't handled.\n" +
                "* Please check to make sure its in the proper format. E.g. '?ping'\n" +
                "* Make sure to add your feature to 'FeatureGraph.kt'\n\n"
    }

    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    init {
        scope.launch {
            delay(Duration.ofSeconds(1))
            println("Incoming message: $args")
            _messagesFlow.emit(
                IncomingChatMessage(
                    command = "?pg",
                    channelId = "123456789",
                    chatUser = CliMockData.defaultCliUser,
                    messageId = Instant.now().toString(),
                    rawMessage = args,
                    arguments = emptyList(),
                    userText = "foo bar"
                ),
            )
        }
    }

    override fun connect() {
        println("${provideEngineName()} is connected")
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        println("Outgoing message: $message")
        println("User sees: ${message.text}")
    }

    override suspend fun eventFlow(): SharedFlow<IncomingChatMessage> = messagesFlow

    override fun provideEngineName(): String = "Cli"
}
