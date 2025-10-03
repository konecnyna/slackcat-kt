package com.slackcat.network

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class NetworkClient(val httpClient: HttpClient) {
    val json = Json { ignoreUnknownKeys = true }

    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
        headers: Map<String, String>
    ): T {
        val jsonResponse = httpClient.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }
        return json.decodeFromString(serializer, jsonResponse.bodyAsText())
    }

    suspend fun fetchString(url: String, headers: Map<String, String>): String {
        val resposne = httpClient.get(url) {
            headers.forEach { (key, value) -> header(key, value) }
        }

        return resposne.bodyAsText()
    }

    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String>
    ): String {
        val response: HttpResponse = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
            headers.forEach { (key, value) -> header(key, value) }
        }
        return response.bodyAsText()
    }
}