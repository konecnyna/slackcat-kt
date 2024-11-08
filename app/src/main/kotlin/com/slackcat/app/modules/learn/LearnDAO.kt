package com.slackcat.app.modules.learn

import org.jetbrains.exposed.sql.Table

class LearnDAO {
    data class LearnRow(val id: Int, val learnedBy: String, val learnKey: Int, val learnText: String)

    object LearnTable : Table() {
        val id = integer("id").autoIncrement()
        val learnedBy = text("learned_by")
        val learnKey = text("learn_key")
        val learnText = text("learn_text")
        override val primaryKey = PrimaryKey(id)
    }
}

data class LearnInsertRow(val learnedBy: String, val learnKey: String, val learnText: String)
