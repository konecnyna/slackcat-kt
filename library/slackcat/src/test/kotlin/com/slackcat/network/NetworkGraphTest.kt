package com.slackcat.network

import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.pluginOrNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class NetworkGraphTest {
    @Test
    fun `shared http client advertises content encoding`() {
        assertNotNull(NetworkGraph.networkClient.httpClient.pluginOrNull(ContentEncoding))
    }
}
