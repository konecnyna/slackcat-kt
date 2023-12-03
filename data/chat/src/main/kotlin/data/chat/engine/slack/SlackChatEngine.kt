package data.chat.engine.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import com.slack.api.model.event.MessageEvent
import data.chat.engine.ChatEngine
import data.chat.models.IncomingChatMessage
import data.chat.models.OutgoingChatMessage
import data.server.Server
import data.server.models.RouteRegistrar
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


class SlackChatEngine(val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) : ChatEngine {
    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    val messagesFlow = _messagesFlow.asSharedFlow()

    val token = System.getenv("SLACK_TOKEN")
    val slack = Slack.getInstance().methods("")


    override fun connect() {
        val registrar = object : RouteRegistrar {
            override fun register(routing: Routing) {
                routing.apply {
                    post("/slack/events") {
                        val slackEvent = call.receive<MessageEvent>()
                        println(slackEvent)
                        when (slackEvent.type) {
                            "message" -> {
                                // Example: respond to a message event
                                val slack = Slack.getInstance()
                                val response = slack.methods().chatPostMessage { req ->
                                    req.channel(slackEvent.channel)
                                        .text("Received your message: ${slackEvent.text}")
                                }
                                call.respondText("Message sent: ${response.message?.text}", ContentType.Text.Plain)
                            }

                            else -> call.respond(200)
                        }
                    }
                    get("/") {
                        call.respondText("foo")
                    }
                }
            }

        }

        Server(listOf(registrar)).start()
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        val response = slack.chatPostMessage { req: ChatPostMessageRequestBuilder ->
            req
                .channel(message.channelId) // Channel ID
                .text(message.text)
        }
    }

    override suspend fun eventFlow() = messagesFlow

    override fun provideEngineName(): String = "Slack"
}


