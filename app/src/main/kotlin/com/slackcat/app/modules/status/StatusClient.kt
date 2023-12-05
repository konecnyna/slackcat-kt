package com.slackcat.app.modules.status

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import kotlinx.serialization.Serializable

class StatusClient {
    @Serializable
    data class SlackStatusResponse(
        val status: String,
        val date_created: String,
        val date_updated: String,
        val active_incidents: List<String?>?,
    )

    suspend fun fetch(): SlackStatusResponse {
        return slackcatNetworkClient.fetch(
            "https://status.slack.com/api/v2.0.0/current",
            SlackStatusResponse.serializer(),
        )
    }
}
