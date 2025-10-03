package com.slackcat.modules

import com.slackcat.presentation.buildRichMessage
import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildMessage

class RadarModule : SlackcatModule() {

    private val radars = listOf(
        Radar("bfl", "CA", "CA - Bakersfield"),
        Radar("bgm", "NY", "NY - Binghamton"),
        Radar("bis", "ND", "ND - Bismarck"),
        Radar("bml", "NH", "NH - Berlin"),
        Radar("bro", "TX", "TX - Brownsville"),
        Radar("bwg", "KY", "KY - Bowling Green"),
        Radar("cad", "MI", "MI - Cadillac"),
        Radar("clt", "NC", "NC - Charlotte"),
        Radar("csg", "GA", "GA - Columbus"),
        Radar("day", "OH", "OH - Dayton"),
        Radar("den", "CO", "CO - Denver"),
        Radar("dsm", "IA", "IA - Des Moines"),
        Radar("eyw", "FL", "FL - Key West"),
        Radar("fcx", "VA", "VA - Roanoke"),
        Radar("hfd", "CT", "CT - Hartford"),
        Radar("jef", "MO", "MO - Jefferson City"),
        Radar("law", "OK", "OK - Lawton"),
        Radar("lbf", "NE", "NE - North Platte"),
        Radar("lit", "AR", "AR - Little Rock"),
        Radar("lwt", "MT", "MT - Lewistown"),
        Radar("msy", "LA", "LA - New Orleans"),
        Radar("myl", "ID", "ID - McCall"),
        Radar("pie", "FL", "FL - Saint Petersburg"),
        Radar("pir", "SD", "SD - Pierre"),
        Radar("prc", "AZ", "AZ - Prescott"),
        Radar("pvu", "UT", "UT - Provo"),
        Radar("rdm", "OR", "OR - Redmond"),
        Radar("riw", "WY", "WY - Riverton"),
        Radar("rno", "NV", "NV - Reno"),
        Radar("row", "NM", "NM - Roswell"),
        Radar("sat", "TX", "TX - San Antonio"),
        Radar("shd", "VA", "VA - Staunton"),
        Radar("sln", "KS", "KS - Salina"),
        Radar("spi", "IL", "IL - Springfield"),
        Radar("stc", "MN", "MN - Saint Cloud"),
        Radar("tiw", "WA", "WA - Tacoma"),
        Radar("tvr", "MS", "MS - Vicksburg"),
    )

    private fun formatUrl(value: String) = "https://s.w-x.co/staticmaps/wu/wxtype/county_loc/$value/animate.png"

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val inputText = incomingChatMessage.userText.uppercase()
        val radarMatch = radars.find { it.state.contains(inputText, ignoreCase = true) }


        val message = when {
            inputText.isEmpty() -> buildRichMessage {
                image(
                    imageUrl = "https://s.w-x.co/staticmaps/wu/wxtype/none/usa/animate.png",
                    altText = "Radar image for United States"
                )
            }
            radarMatch != null -> {
                buildRichMessage {
                    image(
                        imageUrl = formatUrl(radarMatch.value),
                        altText = "Radar image for ${radarMatch.state}"
                    )
                }
            }
            else -> {
                buildRichMessage {
                    section("Radar Not Found")
                    section(
                        """Couldn't find radar for "$inputText". Available radars:\n${radars.sortedBy { it.state }.joinToString("\n") { "- ${it.state}" }}""".trimIndent(),
                    )
                }
            }
        }


        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                message = message
            )
        )
    }

    override fun provideCommand(): String = "radar"

    override fun aliases(): List<String> = listOf("weather", "map", "forecast")

    override fun help(): String = buildMessage {
        title("Radar Help")
        text(
            """
            Usage: `?radar [location]` - Returns the radar map for the specified location.
            Available locations:
            ${radars.joinToString("\n") { it.state }}
            """.trimIndent()
        )
    }

    private data class Radar(
        val value: String,
        val state: String,
        val fullText: String
    )
}