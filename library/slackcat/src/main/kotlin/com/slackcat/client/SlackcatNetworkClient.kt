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
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        return runCatching { networkClient.fetch(url, serializer, headers) }
    }

    suspend inline fun fetchString(url: String, headers: Map<String, String> = emptyMap()): Result<String> = runCatching {
        networkClient.fetchString(url, headers)
    }

    suspend inline fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): Result<String> {
        return runCatching { networkClient.post(url, body, headers) }
    }
}
