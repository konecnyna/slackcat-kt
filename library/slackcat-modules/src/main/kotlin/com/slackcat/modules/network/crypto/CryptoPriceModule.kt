package com.slackcat.modules.network.crypto

import com.slackcat.app.modules.crypto.CryptoPriceClient
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.text
import java.text.DecimalFormat

class CryptoPriceModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val cryptoPriceClient by lazy { CryptoPriceClient(networkClient) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val userText = incomingChatMessage.userText.trim()
        if (userText.isEmpty()) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val cryptoPrice = cryptoPriceClient.getPrice(userText)
        val message =
            if (cryptoPrice != null) {
                // Format the price with commas and preserve precision
                val formattedPrice = formatPrice(cryptoPrice.price)
                "The current price of ${cryptoPrice.ticker}: $$formattedPrice"
            } else {
                "Could not fetch the price for $userText. Please ensure the ticker symbol is correct."
            }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = text(message),
            ),
        )
    }

    private fun formatPrice(price: Double): String {
        return if (price >= 1) {
            // For values >= 1, add commas and no scientific notation
            DecimalFormat("#,###.##").format(price)
        } else {
            // For values < 1, preserve precision (scientific notation for very small numbers)
            DecimalFormat("0.############").format(price)
        }
    }

    override fun help(): String =
        buildMessage {
            title("CryptoPriceModule Help")
            text("Fetch current cryptocurrency prices.")
            text("*Usage:* ?crypto <ticker>")
            text("Example: `?crypto btc` to get the price of Bitcoin.")
        }

    override fun provideCommand(): String = "crypto"
}
