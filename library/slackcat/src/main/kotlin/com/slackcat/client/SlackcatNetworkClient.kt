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
    ): Result<T> {
        return runCatching { networkClient.fetch(url, serializer) }
    }

    suspend inline fun fetchString(url: String): Result<String> = runCatching {
        networkClient.fetchString(url)
    }

    suspend inline fun post(
        url: String,
        body: String,
    ): Result<String> {
        return runCatching { networkClient.post(url, body) }
    }
}
