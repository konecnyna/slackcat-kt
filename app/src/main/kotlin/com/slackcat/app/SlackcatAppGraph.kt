package com.slackcat.app

import com.features.slackcat.client.SlackcatNetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object SlackcatAppGraph {
    val globalScope = CoroutineScope(Dispatchers.IO)

    val slackcatNetworkClient = SlackcatNetworkClient()
}
