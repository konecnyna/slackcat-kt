package com.slackcat.server

import com.slackcat.server.ServerGraph.globalServer
import com.slackcat.server.models.RouteRegistrar
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

class Server(private val routeRegistrars: List<RouteRegistrar>) {
    fun start() {
        globalServer =
            embeddedServer(Netty, port = 8080) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            ignoreUnknownKeys = true
                            isLenient = true
                        },
                    )
                }
                routing {
                    routeRegistrars.forEach { it.register(this) }
                }
            }
        globalServer.start(wait = true)
    }
}
