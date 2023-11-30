package features.chat.status

import data.NetworkClient
import kotlinx.serialization.Serializable


class StatusClient {
    val networkClient = NetworkClient()

    suspend fun fetch(): SlackStatusResponse {
        return networkClient.fetch(
            "https://status.slack.com/api/v2.0.0/current",
            SlackStatusResponse.serializer()
        )
    }
}


@Serializable
data class SlackStatusResponse(
    val status: String,
    val date_created: String,
    val date_updated: String,
    val active_incidents: List<String?>?
)