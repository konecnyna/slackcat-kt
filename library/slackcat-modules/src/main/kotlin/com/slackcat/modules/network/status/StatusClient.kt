package com.slackcat.modules.network.status

import com.slackcat.network.NetworkClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class StatusClient(private val networkClient: NetworkClient) {
    private val json = Json { ignoreUnknownKeys = true }

    enum class Service(val label: String, val url: String, val keywords: List<String>) {
        Slack(label = "Slack", url = "https://status.slack.com/api/v2.0.0/current", keywords = listOf("slack")),
        Github(
            label = "GitHub",
            url = "https://www.githubstatus.com/api/v2/summary.json",
            keywords = listOf("gh", "github"),
        ),
        CircleCi(
            label = "CircleCI",
            url = "https://status.circleci.com/api/v2/summary.json",
            keywords = listOf("circle", "circleci"),
        ),
        CloudFlare(
            label = "CloudFlare",
            url = "https://www.cloudflarestatus.com/api/v2/summary.json",
            keywords = listOf("cf", "cloudflare"),
        ),
    }

    data class Status(
        val service: Service,
        val summary: String,
        val degradedComponents: List<String>,
        val activeIncidents: List<String>,
    ) {
        fun toMessage(): String {
            val parts = mutableListOf<String>()
            parts.add("*${service.label}:* $summary")

            if (degradedComponents.isNotEmpty()) {
                parts.add("Issues: ${degradedComponents.joinToString(", ")}")
            }

            if (activeIncidents.isNotEmpty()) {
                parts.add("Incidents: ${activeIncidents.joinToString(", ")}")
            }

            return parts.joinToString(" | ")
        }
    }

    suspend fun fetch(service: Service): Status? {
        return runCatching {
            val responseString = networkClient.fetchString(service.url, emptyMap())
            when (service) {
                Service.Slack -> parseSlackStatus(service, responseString)
                Service.Github, Service.CircleCi, Service.CloudFlare ->
                    parsePageStatus(service, responseString)
            }
        }.getOrNull()
    }

    private fun parseSlackStatus(
        service: Service,
        responseString: String,
    ): Status {
        val response = json.decodeFromString(SlackStatusResponse.serializer(), responseString)
        val incidents =
            response.activeIncidents
                ?.filterNotNull()
                ?.filter { it.isNotBlank() }
                ?: emptyList()

        return Status(
            service = service,
            summary = response.status,
            degradedComponents = emptyList(),
            activeIncidents = incidents,
        )
    }

    private fun parsePageStatus(
        service: Service,
        responseString: String,
    ): Status {
        val response = json.decodeFromString(PageSummaryResponse.serializer(), responseString)

        val degraded =
            response.components
                .filter { it.status != "operational" && it.showcase }
                .map { "${it.name}: ${it.status.replace("_", " ")}" }

        val incidents =
            response.incidents.map { incident ->
                val status = incident.status.replace("_", " ")
                "${incident.name} ($status)"
            }

        return Status(
            service = service,
            summary = response.status.description,
            degradedComponents = degraded,
            activeIncidents = incidents,
        )
    }
}

@Serializable
data class SlackStatusResponse(
    val status: String,
    @SerialName("date_created") val dateCreated: String? = null,
    @SerialName("date_updated") val dateUpdated: String? = null,
    @SerialName("active_incidents") val activeIncidents: List<String?>? = null,
)

@Serializable
data class PageSummaryResponse(
    val page: Page,
    val status: StatusInfo,
    val components: List<Component> = emptyList(),
    val incidents: List<Incident> = emptyList(),
) {
    @Serializable
    data class Page(
        val id: String,
        val name: String,
        val url: String,
        @SerialName("time_zone") val timeZone: String,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    data class StatusInfo(
        val indicator: String,
        val description: String,
    )

    @Serializable
    data class Component(
        val id: String,
        val name: String,
        val status: String,
        val description: String? = null,
        val showcase: Boolean = false,
    )

    @Serializable
    data class Incident(
        val id: String,
        val name: String,
        val status: String,
        val impact: String,
    )
}
