package com.slackcat.app.modules.summon

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SummonClient {

    suspend fun fetch(): {
        return slackcatNetworkClient.fetchString(service.url).getOrNull()?.let {

        }
    }
}
