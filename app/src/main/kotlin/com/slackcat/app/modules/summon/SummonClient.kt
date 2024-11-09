package com.slackcat.app.modules.summon

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URL
import java.util.regex.Pattern

class SummonClient {
    private val baseUrl = "https://duckduckgo.com"
    private val headers = mapOf(
        "authority" to "duckduckgo.com",
        "accept" to "application/json, text/javascript, */*; q=0.01",
        "sec-fetch-dest" to "empty",
        "x-requested-with" to "XMLHttpRequest",
        "user-agent" to "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36",
        "sec-fetch-site" to "same-origin",
        "sec-fetch-mode" to "cors",
        "referer" to "https://duckduckgo.com/",
        "accept-language" to "en-US,en;q=0.9"
    )

    suspend fun getHtml(query: String): List<SummonImage> {
        val vqdToken = getToken("$baseUrl/?q=$query") ?: return emptyList()
        val params = mapOf(
            "l" to "us-en",
            "o" to "json",
            "q" to query,
            "vqd" to vqdToken,
            "f" to ",,,",
            "p" to "1",
            "v7exp" to "a"
        )

        var requestUrl = baseUrl + "/i.js" + "?" + params.entries.joinToString("&") { "${it.key}=${it.value}" }
        return buildList {
            try {
                while (true) {
                    // Fetch response as string from URL
                    val response = slackcatNetworkClient.fetchString(requestUrl, headers).getOrNull() ?: break

                    // Parse the JSON response
                    val jsonObject = Json.parseToJsonElement(response).jsonObject
                    val results = jsonObject["results"]?.jsonArray

                    // Print out results
                    results?.forEach {
                        add(
                            SummonImage(
                                image = it.jsonObject["image"]?.jsonPrimitive?.content ?: "",
                                thumbnail = it.jsonObject["thumbnail"]?.jsonPrimitive?.content ?: "",
                                title = it.jsonObject["title"]?.jsonPrimitive?.content ?: "",
                                source = it.jsonObject["url"]?.jsonPrimitive?.content ?: ""
                            )
                        )
                    }

                    // Check for "next" field to continue pagination
                    val nextUrl = jsonObject["next"]?.jsonPrimitive?.contentOrNull
                    if (nextUrl == null) {
                        break
                    } else {
                        requestUrl = "$baseUrl/$nextUrl"
                    }
                }
            } catch (exception: Exception) {

            }
        }
    }


    private suspend fun getToken(url: String): String? {
        val htmlContent = slackcatNetworkClient.fetchString(url, headers).getOrNull() ?: return null
        val pattern = Pattern.compile("vqd=([\\d-]+)&")
        val matcher = pattern.matcher(htmlContent)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}


data class SummonImage(
    val image: String,
    val thumbnail: String,
    val title: String,
    val source: String
)