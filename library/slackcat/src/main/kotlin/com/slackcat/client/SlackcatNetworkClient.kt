package com.slackcat.client

import com.slackcat.network.NetworkGraph
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.KSerializer

class SlackcatNetworkClient {
    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
    ): T {
        return NetworkGraph.networkClient.fetch(url, serializer)
    }

    suspend inline fun post(
        url: String,
        body: String,
    ): String {
        return NetworkGraph.networkClient.post(url, body)
    }
}
