package com.slackcat.modules.simple

import com.slackcat.chat.models.IncomingChatMessage
import com.slackcat.chat.models.OutgoingChatMessage
import com.slackcat.common.RichTextMessage
import com.slackcat.internal.Router
import com.slackcat.models.SlackcatModule
import com.slackcat.presentation.buildRichMessage

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
                message = modulesList,
                threadId = incomingChatMessage.threadId,
            ),
        )
    }

    private fun buildModulesList(): RichTextMessage {
        val activeModules =
            router.getAllModules()
                .filter { it !is ModulesModule } // Exclude self from list

        val grouped = activeModules.groupBy { getModuleCategory(it) }

        return buildRichMessage {
            section("*📦 Active Slackcat Modules*")
            divider()

            grouped.forEach { (category, modules) ->
                val emoji = getCategoryEmoji(category)
                section("*$emoji $category*")

                modules.sortedBy { it.provideCommand() }.forEach { module ->
                    val command = module.provideCommand()
                    val aliases = module.aliases()
                    val aliasText =
                        if (aliases.isNotEmpty()) {
                            " (aliases: ${aliases.joinToString(", ")})"
                        } else {
                            ""
                        }
                    section("  • `?$command`$aliasText")
                }

                divider()
            }

            section("*Total: ${activeModules.size} modules*")
            section("_Use `?<command> --help` for more info about a specific module_")
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

    override fun provideCommand(): String = "modules"

    override fun aliases(): List<String> = listOf("commands", "list")

    override fun help(): String =
        """
        *ModulesModule Help*
        Lists all active modules in the bot.

        Usage: `?modules`
        Aliases: `?commands`, `?list`

        This will display all available commands grouped by category,
        along with any aliases they might have.
        """.trimIndent()
}
