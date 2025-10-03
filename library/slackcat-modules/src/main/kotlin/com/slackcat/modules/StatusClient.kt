package com.slackcat.modules

import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class StatusClient(private val networkClient: NetworkClient) {
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
        return runCatching {
            val responseString = networkClient.fetchString(service.url, emptyMap())
            when (service) {
                Service.Slack -> {
                    val slackResponse = Json.decodeFromString(SlackStatusResponse.serializer(), responseString)
                    Status(
                        service = service,
                        status = slackResponse.status,
                        updatedAt = slackResponse.dateUpdated.toString() ?: "unknown",
                    )
                }

                Service.Github, Service.CircleCi, Service.CloudFlare -> {
                    val githubResponse = Json.decodeFromString(PageStatusResponse.serializer(), responseString)
                    Status(
                        service = service,
                        status = githubResponse.status.description,
                        updatedAt = githubResponse.page.updatedAt,
                    )
                }
            }
        }.getOrNull()
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
