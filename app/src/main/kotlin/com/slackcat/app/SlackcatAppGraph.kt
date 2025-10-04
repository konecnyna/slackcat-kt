package com.slackcat.app

import com.slackcat.client.SlackcatNetworkClient
import com.slackcat.network.NetworkGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object SlackcatAppGraph {

    val ENV =
        when (System.getenv("ENV")) {
            "PRODUCTION" -> Environment.Production
            else -> Environment.Development
        }
}

enum class Environment { Production, Development }
