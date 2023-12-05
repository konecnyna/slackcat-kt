package com.slackcat.server

import io.ktor.server.netty.NettyApplicationEngine

object ServerGraph {
    lateinit var globalServer: NettyApplicationEngine
}
