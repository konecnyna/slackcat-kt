package com.slackcat.common

object CommandParser {
    fun extractCommand(input: String): String? {
        val regex = "\\?(\\S+)".toRegex()
        val matchResult = regex.find(input)
        return matchResult?.groups?.get(1)?.value
    }

    fun validateCommandMessage(message: String): Boolean {
        return when {
            message.isEmpty() -> false
            message[0] != '?' -> false
            else -> true
        }
    }

    fun extractArguments(input: String): List<String> {
        val argumentRegex = "--\\S+".toRegex()
        return argumentRegex.findAll(input).map { it.value }.toList()
    }

    fun extractUserText(rawMessage: String): String {
        val command = extractCommand(rawMessage)
        return rawMessage.replaceFirst("\\?\\s*${Regex.escape(command ?: "")}".toRegex(), "").trim()
    }
}
