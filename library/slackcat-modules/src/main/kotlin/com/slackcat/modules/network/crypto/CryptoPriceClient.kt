package com.slackcat.app.modules.crypto

import com.slackcat.network.NetworkClient
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

class CryptoPriceClient(private val networkClient: NetworkClient) {
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
        return runCatching {
            val result =
                networkClient.fetch(
                    url = url,
                    serializer =
                        MapSerializer(
                            String.serializer(),
                            Double.serializer(),
                        ),
                    headers = emptyMap(),
                )
            CryptoPrice(ticker.uppercase(), result["USD"]!!)
        }.getOrNull()
    }
}

data class CryptoPrice(
    val ticker: String,
    val price: Double,
)
