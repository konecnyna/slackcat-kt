package com.slackcat.modules

import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.net.URL
import java.net.URLEncoder
import java.util.regex.Pattern

class SummonClient(private val networkClient: NetworkClient) {
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

    suspend fun getHtml(query: String, animated: Boolean): List<SummonImage> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val vqdToken = getToken("$baseUrl/?q=$encodedQuery") ?: return emptyList()
        val filters = if (animated) {
            ",,,type:gif,,"
        } else {
            ",,,"
        }

        val params = mutableMapOf(
            "l" to "us-en",       // "l" specifies the language; "us-en" means US English results.
            "o" to "json",        // "o" indicates the output format; "json" returns the results in JSON format.
            "q" to query, // "q" is the search query itself, which you’re passing from the function argument.
            "vqd" to vqdToken,    // "vqd" is a unique token DuckDuckGo uses to validate the query; this token must be retrieved before sending the search request.
            "f" to filters,             // "f" is typically used to apply search filters; leaving it as ",,," indicates no specific filters.
            "p" to "1",           // "p" is for "safe search" filtering; "1" enables moderate filtering, while "0" disables it.
            "v7exp" to "a",       // "v7exp" is used for experimental DuckDuckGo features; "a" might indicate a default or "beta" experiment.

        )

        val requestUrl = baseUrl + "/i.js" + "?" + params.entries.joinToString("&") { 
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}" 
        }
        return buildList {
            try {
                // Fetch response as string from URL
                val response = networkClient.fetchString(requestUrl, headers)

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
            } catch (exception: Exception) {

            }
        }.filter {
            when (animated) {
                true -> it.image.endsWith("gif")
                false -> true
            }
        }

    }


    private suspend fun getToken(url: String): String? {
        val htmlContent = runCatching {
            networkClient.fetchString(url, headers)
        }.getOrNull() ?: return null
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