package com.slackcat.modules.network.emoji

import com.slackcat.network.NetworkClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class EmojiClient(private val networkClient: NetworkClient) {
    private val emojisJsonUrl = "https://gist.githubusercontent.com/konecnyna/9968c5a3457b4ef39a824222269f82f3/raw/ae599516d75899365318ce882f5be165a5083d2f/emojis.json"
    private var emojiMap: Map<String,String> = mutableMapOf()


    suspend fun fetchEmoji(emojiKey: String): String? {
        if (emojiMap.isEmpty()) {
            runCatching {
                val jsonString = networkClient.fetchString(emojisJsonUrl, emptyMap())
                emojiMap = Json.decodeFromString(jsonString)
            }
        }

        return emojiMap[emojiKey]
    }
}
