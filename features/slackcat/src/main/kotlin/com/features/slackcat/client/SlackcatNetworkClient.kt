package com.features.slackcat.client

import com.slackcat.network.NetworkGraph
import kotlinx.serialization.KSerializer

class SlackcatNetworkClient {
    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
    ): T {
        return NetworkGraph.networkClient.fetch(url, serializer)
    }
}
