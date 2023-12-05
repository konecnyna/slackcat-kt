package com.slackcat.server.models

import io.ktor.server.routing.Routing

interface RouteRegistrar {
    fun register(routing: Routing)
}
