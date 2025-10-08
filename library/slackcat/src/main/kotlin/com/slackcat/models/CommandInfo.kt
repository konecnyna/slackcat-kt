package com.slackcat.models

/**
 * Information about a module's command and its aliases.
 *
 * @property command The primary command name (without the ? prefix)
 * @property aliases Alternative command names that can be used to invoke this module
 */
data class CommandInfo(
    val command: String,
    val aliases: List<String> = emptyList(),
)
