package data.server

import data.server.ServerGraph.globalServer
import data.server.models.RouteRegistrar
import io.ktor.server.netty.Netty
import io.ktor.server.routing.*
import io.ktor.server.engine.*

class Server(private val routeRegistrars: List<RouteRegistrar>) {
    fun start() {
        globalServer = embeddedServer(Netty, port = 8080) {
            routing {
                routeRegistrars.forEach { it.register(this) }
            }
        }
        globalServer.start(wait = true)
    }
}