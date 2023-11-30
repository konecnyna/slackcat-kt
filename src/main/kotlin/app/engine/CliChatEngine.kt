package app.engine

import app.App
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration

class CliChatEngine(private val args: String) : ChatEngine {
    companion object {
        val commandNotHandledErrorMessage =
            "The incoming message wasn't handled. Please check to make sure its in the proper format. E.g. '?ping'"
    }


    private val _messagesFlow = MutableSharedFlow<String>()
    private val messagesFlow = _messagesFlow.asSharedFlow()

    init {
        App.globalScope.launch {
            delay(Duration.ofSeconds(1))
            println("Incoming message: $args")
            _messagesFlow.emit(args)
        }
    }

    override suspend fun connect() {
        println("${provideEngineName()} is connected")
    }

    override suspend fun sendMessage(message: String) = println("Outgoing message: $message")
    override suspend fun disconnect() {
        /** no op **/
    }

    override suspend fun eventFlow(): SharedFlow<String> = messagesFlow
    override fun provideEngineName(): String = "Cli"
}