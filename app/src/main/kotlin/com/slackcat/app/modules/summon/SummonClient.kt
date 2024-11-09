package com.slackcat.app.modules.summon

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SummonClient {

    suspend fun getHtml() {
        val searchTerm =  "bananas"
        val numImages = 10

        val query = searchTerm.split(" ").joinToString("+")
        val url = "https://www.google.com/search?q=$query&source=lnms&tbm=isch"
        val headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.134 Safari/537.36")

        val htmlContent = slackcatNetworkClient.fetchString(url, headers).getOrNull() ?: return
        val regex = Regex("\"ou\":\"(https://[^\"]*)\"")

        val images = regex.findAll(htmlContent).map { it.groupValues[1] }.take(numImages).toList()

        images.forEach { imageUrl ->
            println(imageUrl)
        }

    }

}
