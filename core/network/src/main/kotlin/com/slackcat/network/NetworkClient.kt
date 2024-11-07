package com.slackcat.network

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class NetworkClient(val httpClient: HttpClient) {
    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
    ): T {
        val jsonResponse = httpClient.get(url)
        return Json { ignoreUnknownKeys = true }.decodeFromString(serializer, jsonResponse.bodyAsText())
    }

    suspend inline fun post(
        url: String,
        body: String,
    ): String {
        val response: HttpResponse =
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        return response.bodyAsText()
    }
}
