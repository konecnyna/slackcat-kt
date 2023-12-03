package core.server

import core.server.ServerGraph.globalServer
import core.server.models.RouteRegistrar
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

class Server(private val routeRegistrars: List<RouteRegistrar>) {
    fun start() {
        globalServer = embeddedServer(Netty, port = 8080) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            routing {
                routeRegistrars.forEach { it.register(this) }
            }
        }
        globalServer.start(wait = true)
    }
}