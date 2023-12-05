package com.slackcat.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json

object NetworkGraph {
    val networkClient = NetworkClient(
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    Json {
                        // Configure the JSON settings if needed
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                }

                install(Logging) {
                    logger =
                        object : Logger {
                            override fun log(message: String) {
                                println("\n--------NetworkRequest-----------\n$message\n------------EndRequest-----------\n")
                            }
                        }
                    level = LogLevel.BODY
                }
            },
        )
}
