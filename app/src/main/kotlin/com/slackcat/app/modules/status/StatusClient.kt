package com.slackcat.app.modules.status

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class StatusClient {
    @Serializable
    data class StatusResponse(
        val status: String,
        val date_created: String? = null,
        val date_updated: String? = null,
        val active_incidents: List<String?>? = null,
        val service: String,
    )

    enum class Service(val label: String, val url: String, val arguments: List<String>) {
        Slack(label = "Slack", url = "https://status.slack.com/api/v2.0.0/current", arguments = listOf("--slack")),
        Github(
            label = "Github",
            url = "https://www.githubstatus.com/api/v2/status.json",
            arguments = listOf("--gh", "--github"),
        ),
        CircleCi(
            label = "CircleCi",
            url = "https://status.circleci.com/api/v2/status.json",
            arguments = listOf("--circle", "--circle-ci"),
        ),
        CloudFlare(
            label = "CloudFlare",
            url = "https://www.cloudflarestatus.com/api/v2/status.json",
            arguments = listOf("--cf", "--cloud-flare"),
        ),
    }

    data class Status(
        val service: Service,
        val status: String,
        val updatedAt: String,
    ) {
        fun toMessage() =
            buildMessage {
                text("*${service.label} status:* $status")
            }
    }

    suspend fun fetch(service: Service): Status? {
        return slackcatNetworkClient.fetchString(service.url).getOrNull()?.let {
            when (service) {
                Service.Slack -> {
                    val slackResponse = Json.decodeFromString(SlackStatusResponse.serializer(), it)
                    Status(
                        service = service,
                        status = slackResponse.status,
                        updatedAt = slackResponse.dateUpdated.toString() ?: "unknown",
                    )
                }

                Service.Github, Service.CircleCi, Service.CloudFlare -> {
                    val githubResponse = Json.decodeFromString(PageStatusResponse.serializer(), it)
                    Status(
                        service = service,
                        status = githubResponse.status.description,
                        updatedAt = githubResponse.page.updatedAt,
                    )
                }
            }
        }
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
data class PageStatusResponse(
    val page: Page,
    val status: Status,
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
    data class Status(
        val indicator: String,
        val description: String,
    )
}
