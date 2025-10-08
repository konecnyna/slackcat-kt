package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.BotMessage
import com.slackcat.common.buildMessage
import com.slackcat.internal.Router
import com.slackcat.models.CommandInfo
import com.slackcat.models.SlackcatModule

/**
 * Module that lists all active modules in the bot.
 * Responds with a thread showing all commands and their aliases.
 *
 * Queries the Router at runtime to get the current list of active modules.
 */
class ModulesModule(
    private val router: Router,
) : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val modulesList = buildModulesList()

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = modulesList,
                threadId = incomingChatMessage.threadId,
            ),
        )
    }

    private fun buildModulesList(): BotMessage {
        val activeModules =
            router.getAllModules()
                .filter { it !is ModulesModule } // Exclude self from list

        val grouped = activeModules.groupBy { getModuleCategory(it) }

        return buildMessage {
            text("*📦 Active Slackcat Modules*")
            divider()

            grouped.forEach { (category, modules) ->
                val emoji = getCategoryEmoji(category)
                text("*$emoji $category*")

                modules.sortedBy { it.commandInfo().command }.forEach { module ->
                    val commandInfo = module.commandInfo()
                    val command = commandInfo.command
                    val aliases = commandInfo.aliases
                    val aliasText =
                        if (aliases.isNotEmpty()) {
                            " (aliases: ${aliases.joinToString(", ")})"
                        } else {
                            ""
                        }
                    text("  • `?$command`$aliasText")
                }

                divider()
            }

            text("*Total: ${activeModules.size} modules*")
            text("_Use `?<command> --help` for more info about a specific module_")
        }
    }

    private fun getModuleCategory(module: SlackcatModule): String {
        val packageName = module::class.java.packageName
        return when {
            packageName.contains(".simple") -> "Simple"
            packageName.contains(".storage") -> "Storage"
            packageName.contains(".network") -> "Network"
            packageName.contains(".app.modules") -> "App Specific"
            else -> "Other"
        }
    }

    private fun getCategoryEmoji(category: String): String =
        when (category) {
            "Simple" -> "⚡"
            "Storage" -> "💾"
            "Network" -> "🌐"
            "App Specific" -> "🎯"
            else -> "📌"
        }

    override fun commandInfo() =
        CommandInfo(
            command = "modules",
            aliases = listOf("commands", "list"),
        )

    override fun help(): BotMessage =
        buildMessage {
            heading("ModulesModule Help")
            text("Lists all active modules in the bot.")
            text("")
            text("Usage: `?modules`")
            text("Aliases: `?commands`, `?list`")
            text("")
            text("This will display all available commands grouped by category,")
            text("along with any aliases they might have.")
        }
}
