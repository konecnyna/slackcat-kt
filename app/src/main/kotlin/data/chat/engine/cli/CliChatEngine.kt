package data.chat.engine.cli

import app.AppGraph.globalScope
import data.chat.engine.ChatEngine
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.time.Instant

class CliChatEngine(private val args: String) : ChatEngine {
    companion object {
        val commandNotHandledErrorMessage =
            "\n\nThe incoming message wasn't handled.\n* Please check to make sure its in the proper format. E.g. '?ping'\n* Make sure to add your feature to 'FeatureGraph.kt'\n\n"
    }

    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    init {
        globalScope.launch {
            delay(Duration.ofSeconds(1))
            println("Incoming message: $args")
            _messagesFlow.emit(
                IncomingChatMessage(
                    channelId = "42069",
                    chatUser = CliMockData.defaultCliUser,
                    messageId = Instant.now().toString(),
                    rawMessage = args,
                )
            )
        }
    }

    override suspend fun connect() {
        println("${provideEngineName()} is connected")
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) = println("Outgoing message: $message")
    override suspend fun disconnect() {
        /** no op **/
    }

    override suspend fun eventFlow(): SharedFlow<IncomingChatMessage> = messagesFlow
    override fun provideEngineName(): String = "Cli"
}