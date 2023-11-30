package data

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

class NetworkClient {
    companion object {
        val client = HttpClient {
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
    }

    suspend inline fun <reified T> fetch(url: String, serializer: KSerializer<T>): T {
        val jsonResponse = client.get<String>(url)
        return Json { ignoreUnknownKeys = true }.decodeFromString(serializer, jsonResponse)
    }
}


interface RemoteResponse