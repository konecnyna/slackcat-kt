package com.slackcat.client

import com.slackcat.network.NetworkClient
import com.slackcat.network.NetworkGraph
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.KSerializer

class SlackcatNetworkClient() {
    val networkClient: NetworkClient = NetworkGraph.networkClient

    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
    ): T {
        return networkClient.fetch(url, serializer)
    }

    suspend inline fun fetchString(url: String): String = networkClient.fetchString(url)

    suspend inline fun post(
        url: String,
        body: String,
    ): String {
        return networkClient.post(url, body)
    }
}
