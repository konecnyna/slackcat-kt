package data.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.json.Json

/**
 * A very simple global singleton dependency graph.
 * For a real app, you would use something like Hilt/Dagger instead.
 */
object NetworkGraph {
    val networkClient = NetworkClient(HttpClient(CIO) {
        install(ContentNegotiation) {
            Json {
                // Configure the JSON settings if needed
                isLenient = true
                ignoreUnknownKeys = true
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) { println("\n--------NetworkRequest-----------\n$message\n------------EndRequest-----------\n") }
            }
            level = LogLevel.BODY
        }
    })
}