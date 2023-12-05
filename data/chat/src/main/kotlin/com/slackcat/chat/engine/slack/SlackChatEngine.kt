package com.slackcat.chat.engine.slack

import com.slack.api.Slack
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import com.slackcat.chat.engine.ChatEngine
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.server.Server
import com.slackcat.server.models.RouteRegistrar
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SlackChatEngine(val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) : ChatEngine {
    private val _messagesFlow = MutableSharedFlow<IncomingChatMessage>()
    val messagesFlow = _messagesFlow.asSharedFlow()

    val token = System.getenv("SLACK_TOKEN")
    val slack = Slack.getInstance().methods(token)

    override fun connect() {
        val registrar =
            object : RouteRegistrar {
                override fun register(routing: Routing) {
                    routing.apply {
                        post("/slack/events") {
                            try {
//                            val slackEvent = call.receive<SlackEvent>()
//                            println(slackEvent)
//                            when (slackEvent.type) {
//                                "url_verification" -> {
// //                                    call.respond(
// //                                        mapOf("challenge" to slackEvent.challenge)
// //                                    )
//                                }
//
//                                else -> call.respond(200)
//                            }
                            } catch (exception: Exception) {
                                exception.printStackTrace()
                            }
                        }
                    }
                }
            }

        Server(listOf(registrar)).start()
    }

    override suspend fun sendMessage(message: OutgoingChatMessage) {
        val response =
            slack.chatPostMessage { req: ChatPostMessageRequestBuilder ->
                req
                    .channel(message.channelId) // Channel ID
                    .text(message.text)
            }
    }

    override suspend fun eventFlow() = messagesFlow

    override fun provideEngineName(): String = "Slack"
}
