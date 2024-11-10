package com.slackcat.app.modules.emoji

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class EmojiClient {
    private val emojisJsonUrl = "https://gist.githubusercontent.com/konecnyna/9968c5a3457b4ef39a824222269f82f3/raw/ae599516d75899365318ce882f5be165a5083d2f/emojis.json"
    private var emojiMap: Map<String,String> = mutableMapOf()


    suspend fun fetchEmoji(emojiKey: String): String? {
        if (emojiMap.isEmpty()) {
            slackcatNetworkClient.fetchString(emojisJsonUrl).getOrNull()?.let {
                emojiMap = Json.decodeFromString(it)
            }
        }

        return emojiMap[emojiKey]
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
