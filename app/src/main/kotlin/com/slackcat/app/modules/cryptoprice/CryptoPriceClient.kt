package com.slackcat.app.modules.crypto

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class CryptoPriceClient {

    private val priceCache = mutableMapOf<String, CryptoPrice>()

    suspend fun getPrice(ticker: String): CryptoPrice? {
        val cachedPrice = priceCache[ticker.lowercase()]
        if (cachedPrice != null) {
            return cachedPrice
        }

        val price = fetchPriceFromApi(ticker) ?: return null
        priceCache[ticker.lowercase()] = price
        return price
    }

    private suspend fun fetchPriceFromApi(ticker: String): CryptoPrice? {
        val url = "https://min-api.cryptocompare.com/data/price?fsym=${ticker.toUpperCase()}&tsyms=USD"
        val result = slackcatNetworkClient.fetch(
            url = url,
            serializer = MapSerializer(
                String.serializer(),
                Double.serializer()
            )
        ).getOrNull()

        return result?.let {
            CryptoPrice(ticker.uppercase(), it["USD"]!!)
        }
    }
}


data class CryptoPrice(
    val ticker: String,
    val price: Double
)