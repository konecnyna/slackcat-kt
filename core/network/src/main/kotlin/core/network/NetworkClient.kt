package core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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
}
