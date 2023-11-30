package data.network

import data.chat.engine.ChatEngine
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*

/**
 * A very simple global singleton dependency graph.
 *
 * For a real app, you would use something like Hilt/Dagger instead.
 */
object NetworkGraph {
    val networkClient = NetworkClient(HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    })
}