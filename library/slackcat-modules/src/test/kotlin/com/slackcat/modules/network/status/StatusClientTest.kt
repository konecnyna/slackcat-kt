package com.slackcat.modules.network.status

import com.slackcat.network.NetworkClient
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusClientTest {
    private val networkClient = mockk<NetworkClient>()
    private val statusClient = StatusClient(networkClient)

    @Test
    fun `fetch Claude status with incidents`() =
        runTest {
            val mockJsonResponse =
                """
                {
                  "page": {
                    "id": "anthropic",
                    "name": "Anthropic",
                    "url": "https://status.anthropic.com",
                    "time_zone": "UTC",
                    "updated_at": "2026-03-17T12:00:00Z"
                  },
                  "status": {
                    "indicator": "none",
                    "description": "All Systems Operational"
                  },
                  "incidents": [
                    {
                      "id": "mhnzmndv58bt",
                      "name": "Elevated errors on Claude Opus 4.6",
                      "status": "monitoring",
                      "created_at": "2026-03-17T19:47:30Z",
                      "updated_at": "2026-03-17T20:02:43Z",
                      "incident_updates": [
                        {
                          "id": "update1",
                          "status": "monitoring",
                          "body": "A fix has been implemented and we are monitoring the results.",
                          "created_at": "2026-03-17T20:02:43Z",
                          "updated_at": "2026-03-17T20:02:43Z"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()

            coEvery { networkClient.fetchString(StatusClient.Service.Claude.url, emptyMap()) } returns mockJsonResponse

            val result = statusClient.fetch(StatusClient.Service.Claude)

            assertNotNull(result)
            assertEquals("Claude (Anthropic)", result?.service?.label)
            assertEquals("All Systems Operational", result?.status)
            assertEquals(1, result?.incidents?.size)
            assertEquals("Elevated errors on Claude Opus 4.6", result?.incidents?.first()?.name)
            assertEquals("monitoring", result?.incidents?.first()?.status)
            assertEquals(
                "A fix has been implemented and we are monitoring the results.",
                result?.incidents?.first()?.incidentUpdates?.first()?.body,
            )
        }

    @Test
    fun `fetch GitHub status with minor outage`() =
        runTest {
            val mockJsonResponse =
                """
                {
                  "page": {
                    "id": "github",
                    "name": "GitHub",
                    "url": "https://www.githubstatus.com",
                    "time_zone": "UTC",
                    "updated_at": "2024-03-20T12:00:00Z"
                  },
                  "status": {
                    "indicator": "minor",
                    "description": "Partial System Outage"
                  },
                  "incidents": []
                }
                """.trimIndent()

            coEvery { networkClient.fetchString(StatusClient.Service.Github.url, emptyMap()) } returns mockJsonResponse

            val result = statusClient.fetch(StatusClient.Service.Github)

            assertNotNull(result)
            assertEquals("GitHub", result?.service?.label)
            assertEquals("Partial System Outage", result?.status)
            assertEquals("minor", result?.indicator)
            assertTrue(result?.incidents?.isEmpty() ?: false)
        }
}
