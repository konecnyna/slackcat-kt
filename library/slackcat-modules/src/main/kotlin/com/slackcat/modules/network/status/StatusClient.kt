package com.slackcat.modules.network.status

import com.slackcat.common.MessageStyle
import com.slackcat.common.buildMessage
import com.slackcat.network.NetworkClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

open class StatusClient(
    private val networkClient: NetworkClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    enum class Service(
        val label: String,
        val url: String,
        val statusPageUrl: String,
        val arguments: List<String>,
    ) {
        Slack(
            label = "Slack",
            url = "https://slack-status.com/api/v2.0.0/current",
            statusPageUrl = "https://status.slack.com",
            arguments = listOf("--slack", "slack"),
        ),
        Github(
            label = "GitHub",
            url = "https://www.githubstatus.com/api/v2/summary.json",
            statusPageUrl = "https://www.githubstatus.com",
            arguments = listOf("--gh", "--github", "github", "gh"),
        ),
        CircleCi(
            label = "CircleCI",
            url = "https://status.circleci.com/api/v2/summary.json",
            statusPageUrl = "https://status.circleci.com",
            arguments = listOf("--circle", "--circle-ci", "circle", "circleci"),
        ),
        CloudFlare(
            label = "Cloudflare",
            url = "https://www.cloudflarestatus.com/api/v2/summary.json",
            statusPageUrl = "https://www.cloudflarestatus.com",
            arguments = listOf("--cf", "--cloud-flare", "cloudflare", "cf"),
        ),
        Claude(
            label = "Claude (Anthropic)",
            url = "https://status.claude.com/api/v2/summary.json",
            statusPageUrl = "https://status.claude.com",
            arguments = listOf("--claude", "--anthropic", "claude", "anthropic"),
        ),
        Auth0(
            label = "Auth0",
            url = "https://status.auth0.com/api/v2/summary.json",
            statusPageUrl = "https://status.auth0.com",
            arguments = listOf("--auth0", "auth0"),
        ),
        OneSignal(
            label = "OneSignal",
            url = "https://status.onesignal.com/api/v2/summary.json",
            statusPageUrl = "https://status.onesignal.com",
            arguments = listOf("--onesignal", "onesignal"),
        ),
    }

    data class Status(
        val service: Service,
        val status: String,
        val indicator: String,
        val updatedAt: String,
        val incidents: List<Incident> = emptyList(),
    ) {
        fun toRichMessage() =
            buildMessage(getMessageStyle(indicator)) {
                val emoji = getStatusEmoji(indicator)
                text(
                    buildString {
                        appendLine("$emoji *${service.label} Status*")
                        appendLine("*Status:* ${formatStatus(status)}")
                        appendLine("*Indicator:* ${indicator.replaceFirstChar { it.uppercase() }}")
                        appendLine("*Updated:* $updatedAt")

                        if (incidents.isNotEmpty()) {
                            appendLine("\n*Active Incidents:*")
                            incidents.take(3).forEach { incident ->
                                val incidentStatus = incident.status.replaceFirstChar { it.uppercase() }
                                appendLine("• *${incident.name}* ($incidentStatus)")
                                incident.incidentUpdates.firstOrNull()?.let { update ->
                                    val truncated = update.body.take(200)
                                    val suffix = if (update.body.length > 200) "..." else ""
                                    appendLine("  _$truncated${suffix}_")
                                }
                            }
                        }

                        append("\n_View details:_ <${service.statusPageUrl}|${service.label} Status Page>")
                    },
                )
            }

        private fun getMessageStyle(indicator: String): MessageStyle =
            when (indicator.lowercase()) {
                "none", "operational", "active" -> MessageStyle.SUCCESS
                "minor" -> MessageStyle.WARNING
                "major", "critical" -> MessageStyle.ERROR
                else -> MessageStyle.INFO
            }

        private fun getStatusEmoji(indicator: String): String =
            when (indicator.lowercase()) {
                "none", "operational" -> "\u2705"
                "minor" -> "\u26A0\uFE0F"
                "major" -> "\uD83D\uDD34"
                "critical" -> "\uD83D\uDEA8"
                "active" -> "\u2705"
                else -> "\u2139\uFE0F"
            }

        private fun formatStatus(status: String): String =
            status
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    suspend fun fetch(service: Service): Status? =
        runCatching {
            val responseString = networkClient.fetchString(service.url, emptyMap())
            when (service) {
                Service.Slack -> {
                    val slackResponse = json.decodeFromString(SlackStatusResponse.serializer(), responseString)
                    val indicator =
                        when (slackResponse.status.lowercase()) {
                            "ok", "active" -> "operational"
                            else -> "incident"
                        }
                    Status(
                        service = service,
                        status = slackResponse.status,
                        indicator = indicator,
                        updatedAt = slackResponse.dateUpdated ?: "Unknown",
                    )
                }
                Service.Github,
                Service.CircleCi,
                Service.CloudFlare,
                Service.Claude,
                Service.Auth0,
                Service.OneSignal,
                -> {
                    val response = json.decodeFromString(PageStatusResponse.serializer(), responseString)
                    Status(
                        service = service,
                        status = response.status.description,
                        indicator = response.status.indicator,
                        updatedAt = response.page.updatedAt,
                        incidents = response.incidents.filter { it.status.lowercase() != "resolved" },
                    )
                }
            }
        }.onFailure {
            println("Error fetching status for ${service.label}: ${it.message}")
            it.printStackTrace()
        }.getOrNull()

    @Serializable
    data class Incident(
        val id: String,
        val name: String,
        val status: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
        @SerialName("incident_updates") val incidentUpdates: List<IncidentUpdate> = emptyList(),
    )

    @Serializable
    data class IncidentUpdate(
        val id: String,
        val status: String,
        val body: String,
        @SerialName("created_at") val createdAt: String,
        @SerialName("updated_at") val updatedAt: String,
    )
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
    val incidents: List<StatusClient.Incident> = emptyList(),
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
