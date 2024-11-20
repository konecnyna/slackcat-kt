package com.slackcat.client

import com.slackcat.network.NetworkClient
import com.slackcat.network.NetworkGraph
import kotlinx.serialization.KSerializer

class SlackcatNetworkClient() {
    val networkClient: NetworkClient = NetworkGraph.networkClient

    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
        headers: Map<String, String> = emptyMap()
    ): Result<T> {
        val result = runCatching { networkClient.fetch(url, serializer, headers) }
        if (result.isFailure) {
            printError(url, result)
        }
        return result
    }

    suspend inline fun fetchString(url: String, headers: Map<String, String> = emptyMap()): Result<String> =
        runCatching {
            networkClient.fetchString(url, headers)
        }

    suspend inline fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): Result<String> {
        return runCatching { networkClient.post(url, body, headers) }
    }


    fun <T> printError(url: String, result: Result<T>) {
        val exception = result.exceptionOrNull()
        println("Error fetching data from URL: $url")
        println("Exception: ${exception?.message}")
        exception?.printStackTrace()
    }
}
