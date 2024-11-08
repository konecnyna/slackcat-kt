package com.slackcat.app.modules.pokecat


import PokemonData
import com.slackcat.app.SlackcatAppGraph
import com.slackcat.app.SlackcatAppGraph.globalScope
import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.app.modules.translate.ApiResponse
import com.slackcat.models.SlackcatModule

import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage

class PokeCatModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        globalScope.launch {
            val baseurl = "https://pokeapi.co/api/v2/pokemon"
            val pokemonIdentifier = incomingChatMessage.userText

            val pokemon = slackcatNetworkClient.fetch("$baseurl/$pokemonIdentifier", PokemonData.serializer())

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

            val jsonObject: JsonObject = Json.parseToJsonElement(blocks).jsonObject
            println(jsonObject)
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    text = "pokemon",
                    blocks = jsonObject,
                    userName = "PokéCat",
                    iconUrl = "https://emoji.slack-edge.com/T07UUET6K51/pokeball/6812d9253feb15f7.png"
                ),
            )
        }
    }


    private fun extractPokemonIdentifier(userText: String): String? {
        val regex = """^\S+\s+(\S+)""".toRegex()
        return regex.find(userText)?.groupValues?.get(1)
    }

    override fun provideCommand(): String = "pokemon"
    override fun help(): String {
        TODO("Not yet implemented")
    }
}
