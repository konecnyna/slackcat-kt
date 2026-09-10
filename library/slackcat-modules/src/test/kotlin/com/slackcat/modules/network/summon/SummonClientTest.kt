package com.slackcat.modules.network.summon

import com.slackcat.network.NetworkClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SummonClientTest {
    private val networkClient = mockk<NetworkClient>()
    private val summonClient = SummonClient(networkClient)
    private val requests = mutableListOf<Pair<String, Map<String, String>>>()

    private val imageResults =
        """{"results":[{"image":"https://example.com/a.jpg","thumbnail":"t","title":"a","url":"u"}]}"""

    private fun stubDuckDuckGo(tokenPage: String) {
        coEvery { networkClient.fetchString(any(), any()) } answers {
            val url = firstArg<String>()
            requests += url to secondArg()
            if (url.contains("/i.js")) imageResults else tokenPage
        }
    }

    @Test
    fun `extracts token from legacy query string form`() =
        runTest {
            stubDuckDuckGo("""<a href="/d.js?q=cat&vqd=4-111&o=json">""")

            val images = summonClient.getHtml("cat", animated = false)

            assertEquals("https://example.com/a.jpg", images.single().image)
            assertTrue(requests.last().first.contains("vqd=4-111"))
        }

    @Test
    fun `extracts token from quoted attribute form`() =
        runTest {
            stubDuckDuckGo("""<script>DDG.deep.initialize('/d.js', vqd="4-222");</script>""")

            val images = summonClient.getHtml("cat", animated = false)

            assertEquals("https://example.com/a.jpg", images.single().image)
            assertTrue(requests.last().first.contains("vqd=4-222"))
        }

    @Test
    fun `returns empty list when no token is present`() =
        runTest {
            stubDuckDuckGo("<html>blocked</html>")

            assertTrue(summonClient.getHtml("cat", animated = false).isEmpty())
            assertEquals(1, requests.size)
        }

    @Test
    fun `does not impersonate a browser`() =
        runTest {
            stubDuckDuckGo("""vqd="4-333"""")

            summonClient.getHtml("cat", animated = false)

            val sentHeaders = requests.flatMap { it.second.keys }.map { it.lowercase() }.toSet()
            val browserOnlyHeaders =
                setOf(
                    "user-agent",
                    "authority",
                    "x-requested-with",
                    "sec-fetch-dest",
                    "sec-fetch-mode",
                    "sec-fetch-site",
                )
            assertTrue(sentHeaders.intersect(browserOnlyHeaders).isEmpty(), "sent browser headers: $sentHeaders")
        }
}
