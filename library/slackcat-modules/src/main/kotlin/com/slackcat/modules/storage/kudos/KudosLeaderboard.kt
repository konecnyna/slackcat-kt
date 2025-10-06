package com.slackcat.modules.storage.kudos

class KudosLeaderboard(private val kudosDAO: KudosDAO) {
    suspend fun getLeaderboardMessage(): String {
        val topKudos = kudosDAO.getTopKudos(10)

        if (topKudos.isEmpty()) {
            return "No kudos have been given yet! Be the first to spread some positivity with `?++ @username`"
        }

        return buildString {
            appendLine(":trophy: *Kudos Leaderboard* :trophy:")
            appendLine()
            topKudos.forEachIndexed { index, kudosRow ->
                val medal =
                    when (index) {
                        0 -> ":first_place_medal:"
                        1 -> ":second_place_medal:"
                        2 -> ":third_place_medal:"
                        else -> "${index + 1}."
                    }
                val plusText = if (kudosRow.count == 1) "plus" else "pluses"
                appendLine("$medal <@${kudosRow.userId}> - ${kudosRow.count} $plusText")
            }
        }.trim()
    }
}
