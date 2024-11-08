package com.slackcat.app.modules.status

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class StatusClient {

    @Serializable
    data class StatusResponse(
        val status: String,
        val date_created: String? = null,
        val date_updated: String? = null,
        val active_incidents: List<String?>? = null,
        val service: String
    )

    enum class Service(val label: String, val url: String, val argument: List<String>) {
        SLACK(label = "Slack", url ="https://status.slack.com/api/v2.0.0/current", argument = listOf("--slack")),
        GITHUB(label = "Github", url ="https://www.githubstatus.com/api/v2/status.json", argument = listOf("--gh","--github")),
//        TWITCH(name = " url ="https://status.twitch.tv/api/v2/status.json", argument = listOf("--twitch")),
//        SPOTIFY( url ="https://spotify.statuspage.io/api/v2/status.json", argument = listOf("--spotify"))
    }

    data class Status(
        val service: Service,
        val status: String,
        val updatedAt: String,
    ) {
        fun toMessage() = buildMessage {
            text("*${service.label} status:* $status")
        }
    }

    suspend fun fetch(service: Service): Status {
        val responseText = slackcatNetworkClient.fetchString(service.url)

        return when (service) {
            Service.SLACK -> {
                val slackResponse = Json.decodeFromString(SlackStatusResponse.serializer(), responseText)
                Status(
                    service = service,
                    status = slackResponse.status,
                    updatedAt = slackResponse.dateUpdated.toString() ?: "unknown"
                )
            }
            Service.GITHUB -> {
                val githubResponse = Json.decodeFromString(GitHubStatusResponse.serializer(), responseText)
                Status(
                    service = service,
                    status = githubResponse.status.description,
                    updatedAt = githubResponse.page.updatedAt
                )
            }
            // Implement cases for TWITCH, SPOTIFY, etc. as needed
            else -> throw IllegalArgumentException("Unsupported service")
        }
    }
}


@Serializable
data class SlackStatusResponse(
    val status: String,
    @SerialName("date_created") val dateCreated: String? = null,
    @SerialName("date_updated") val dateUpdated: String? = null,
    @SerialName("active_incidents") val activeIncidents: List<String?>? = null
)

@Serializable
data class GitHubStatusResponse(
    val page: Page,
    val status: Status
) {
    @Serializable
    data class Page(
        val id: String,
        val name: String,
        val url: String,
        @SerialName("time_zone") val timeZone: String,
        @SerialName("updated_at") val updatedAt: String
    )

    @Serializable
    data class Status(
        val indicator: String,
        val description: String
    )
}
