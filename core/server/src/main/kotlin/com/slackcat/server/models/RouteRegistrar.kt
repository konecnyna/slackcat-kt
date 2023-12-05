package com.slackcat.server.models

import io.ktor.server.routing.*

interface RouteRegistrar {
    fun register(routing: Routing)
}