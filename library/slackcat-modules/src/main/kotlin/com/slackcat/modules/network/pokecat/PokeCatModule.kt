package com.slackcat.modules.network.pokecat
import com.slackcat.chat.models.BotIcon
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.RichTextMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.modules.PokemonData
import com.slackcat.network.NetworkClient
import com.slackcat.presentation.buildMessage
import com.slackcat.presentation.buildRichMessage

class PokeCatModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    val baseurl = "https://pokeapi.co/api/v2/pokemon"

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val pokemonIdentifier = incomingChatMessage.userText
        if (pokemonIdentifier.isBlank()) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val pokemon =
            runCatching {
                networkClient.fetch("$baseurl/$pokemonIdentifier", PokemonData.serializer(), emptyMap())
            }.getOrNull()
        if (pokemon == null) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = buildPokemonMessage(pokemon),
                botName = "PokéCat",
                botIcon =
                    BotIcon.BotImageIcon(
                        "https://emoji.slack-edge.com/T07UUET6K51/pokeball/6812d9253feb15f7.png",
                    ),
            ),
        )
    }

    private fun buildPokemonMessage(pokemon: PokemonData): RichTextMessage {
        return buildRichMessage {
            divider()
            section(
                text =
                    "*${pokemon.name.uppercase()}* \n *HP: ${pokemon.stats[0].base_stat}* \n " +
                        "*Attack: ${pokemon.stats[1].base_stat}* \n *Defense: ${pokemon.stats[2].base_stat}* ",
                imageUrl = pokemon.sprites.front_default,
                altText = pokemon.sprites.front_default,
            )
            divider()
        }
    }

    private fun extractPokemonIdentifier(userText: String): String? {
        val regex = """^\S+\s+(\S+)""".toRegex()
        return regex.find(userText)?.groupValues?.get(1)
    }

    override fun provideCommand(): String = "pokemon"

    override fun help(): String =
        buildMessage {
            title("Pokemon Help")
            text("Get stats on your favorite pokemon")
            text("- Usage: `?pokemon <number>`")
            text("- Ex: `?pokemon 69`")
        }
}
