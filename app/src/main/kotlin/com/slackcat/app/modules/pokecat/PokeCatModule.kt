package com.slackcat.app.modules.pokecat


import PokemonData
import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.chat.models.BotIcon
import com.slackcat.models.SlackcatModule

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.presentation.buildMessage

class PokeCatModule : SlackcatModule() {
    val baseurl = "https://pokeapi.co/api/v2/pokemon"

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val pokemonIdentifier = incomingChatMessage.userText
        if (pokemonIdentifier.isBlank()) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val pokemonResult = slackcatNetworkClient.fetch("$baseurl/$pokemonIdentifier", PokemonData.serializer())
        val pokemon = pokemonResult.getOrNull() ?: return this.postHelpMessage(incomingChatMessage.channelId)
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                text = "pokemon",
                blocks = buildPokemonMessage(pokemon),
                botName = "PokéCat",
                botIcon = BotIcon.BotImageIcon("https://emoji.slack-edge.com/T07UUET6K51/pokeball/6812d9253feb15f7.png"),
            ),
        )

    }


    private fun buildPokemonMessage(pokemon: PokemonData): JsonObject {
        val blocks = """
                {
                    "blocks": [
                        {
                            "type": "divider"
                        },
                        {
                            "type": "section",
                            "text": {
                                "type": "mrkdwn",
                                "text": "*${pokemon.name.uppercase()}* \n *HP: ${pokemon.stats[0].base_stat}* \n *Attack: ${pokemon.stats[1].base_stat}* \n *Defense: ${pokemon.stats[2].base_stat}* "
                            },
                            "accessory": {
                                "type": "image",
                                "image_url": "${pokemon.sprites.front_default}",
                                "alt_text": "${pokemon.sprites.front_default}"
                            }
                        },
                        {
                            "type": "divider"
                        }
                    ]
                }
            """

        return Json.parseToJsonElement(blocks).jsonObject

    }


    private fun extractPokemonIdentifier(userText: String): String? {
        val regex = """^\S+\s+(\S+)""".toRegex()
        return regex.find(userText)?.groupValues?.get(1)
    }

    override fun provideCommand(): String = "pokemon"
    override fun help(): String = buildMessage {
        title("Pokemon Help")
        text("Get stats on your favorite pokemon")
        text("- Usage: `?pokemon <number>`")
        text("- Ex: `?pokemon 69`")
    }
}
