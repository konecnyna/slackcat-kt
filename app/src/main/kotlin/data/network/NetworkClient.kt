package data.network

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class NetworkClient(val httpClient: HttpClient) {
    suspend inline fun <reified T> fetch(url: String, serializer: KSerializer<T>): T {
        val jsonResponse = httpClient.get<String>(url)
        return Json { ignoreUnknownKeys = true }.decodeFromString(serializer, jsonResponse)
    }
}