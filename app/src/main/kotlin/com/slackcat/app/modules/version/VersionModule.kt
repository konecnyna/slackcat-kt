package com.slackcat.app.modules.version

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule
import java.util.Properties

class VersionModule : SlackcatModule() {
    private val appVersion: String by lazy {
        val props = Properties()
        val stream = this::class.java.classLoader.getResourceAsStream("version.properties")
        if (stream != null) {
            props.load(stream)
            props.getProperty("version", "unknown")
        } else {
            "unknown"
        }
    }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        sendMessage(
            incomingChatMessage,
            buildMessage {
                text("Slackcat v$appVersion")
            },
        )
    }

    override fun commandInfo() =
        CommandInfo(
            command = "slackcat-version",
            aliases = listOf("version"),
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("Version Help")
            text("Displays the current running version of Slackcat.")
            text("")
            text("Usage: `?slackcat-version` or `?version`")
        }
}
