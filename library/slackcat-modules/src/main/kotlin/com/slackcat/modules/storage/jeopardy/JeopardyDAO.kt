package com.slackcat.modules.storage.jeopardy

import com.slackcat.database.dbQuery
import com.slackcat.database.dbUpsert
import com.slackcat.network.NetworkClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

class JeopardyDAO(private val networkClient: NetworkClient) {
    data class JeopardyScoreRow(
        val id: Int,
        val userId: String,
        val points: Int,
        val right: Int,
        val wrong: Int
    )
    @Serializable
    data class JeopardyQuestionRow(
        val id: Int,
        val airDate: String,
        val question: String,
        val answer: String,
        val category: String,
        val show_number: String,
        val round: String,
        val value: String
    )

    object JeopardyQuestionsTable : Table() {
        val id = integer("id")
        val airDate = text("air_date")
        val question = text("question")
        val answer = text("answer")
        val category = text("category")
        val show_number = text("show_number")
        val round = text("round")
        val value = text("value")
        override val primaryKey = PrimaryKey(id)
    }

    object JeopardyScoreTable : Table() {
        val id = integer("id").autoIncrement()
        val userId = text("user_id").uniqueIndex()
        val points = integer("points")
        val right = integer("right")
        val wrong = integer("wrong")
        override val primaryKey = PrimaryKey(JeopardyScoreTable.id)
    }

    fun getJeopardyTableLength(): Long {
        return transaction {
            JeopardyQuestionsTable.selectAll().count()
        }
    }

    suspend fun getJeopardyScore(userId: String): JeopardyScoreRow = dbQuery {
        val user = JeopardyScoreTable.select { JeopardyScoreTable.userId eq userId }.single()
        JeopardyScoreRow(
            id = user[JeopardyScoreTable.id],
            userId = user[JeopardyScoreTable.userId],
            points = user[JeopardyScoreTable.points],
            right = user[JeopardyScoreTable.right],
            wrong = user[JeopardyScoreTable.wrong]
        )
    }

    fun getJeopardyQuestion(value: String): JeopardyQuestionRow {
        return transaction {
            val questions = JeopardyQuestionsTable
                .select { JeopardyQuestionsTable.value eq value }
                .map {
                    JeopardyQuestionRow(
                        id = it[JeopardyQuestionsTable.id],
                        airDate = it[JeopardyQuestionsTable.airDate],
                        question = it[JeopardyQuestionsTable.question],
                        answer = it[JeopardyQuestionsTable.answer],
                        category = it[JeopardyQuestionsTable.category],
                        show_number = it[JeopardyQuestionsTable.show_number],
                        round = it[JeopardyQuestionsTable.round],
                        value = it[JeopardyQuestionsTable.value]
                    )
                }
            questions[Random.nextInt(questions.size)]
        }
    }

    fun getJeopardyQuestionById(id: Int): JeopardyQuestionRow {
        return transaction {
            val questions = JeopardyQuestionsTable
                .select { JeopardyQuestionsTable.id eq id }
                .map {
                    JeopardyQuestionRow(
                        id = it[JeopardyQuestionsTable.id],
                        airDate = it[JeopardyQuestionsTable.airDate],
                        question = it[JeopardyQuestionsTable.question],
                        answer = it[JeopardyQuestionsTable.answer],
                        category = it[JeopardyQuestionsTable.category],
                        show_number = it[JeopardyQuestionsTable.show_number],
                        round = it[JeopardyQuestionsTable.round],
                        value = it[JeopardyQuestionsTable.value]
                    )
                }
            questions[Random.nextInt(questions.size)]
        }
    }

    suspend fun hydrateJeopardyQuestions() {
        val gistUrl = "https://gist.githubusercontent.com/JPoirier55/d57ab58d9919eedc169375260a3ea8c3/raw/a215d130685e50d8041f1c5dcbf598437c15b431/slackcat-jeopardy-questions.json"
        val jsonGistFile = networkClient.fetchString(gistUrl, emptyMap())
        return transaction {
            val json = Json { ignoreUnknownKeys = true }
            val jeopardyQuestions: List<JeopardyQuestionRow> = json.decodeFromString(jsonGistFile)
            jeopardyQuestions.forEach { question ->
                JeopardyQuestionsTable.insert {
                    it[JeopardyQuestionsTable.id] = question.id
                    it[JeopardyQuestionsTable.airDate] = question.airDate
                    it[JeopardyQuestionsTable.question] = question.question
                    it[JeopardyQuestionsTable.answer] = question.answer
                    it[JeopardyQuestionsTable.category] = question.category
                    it[JeopardyQuestionsTable.show_number] = question.show_number
                    it[JeopardyQuestionsTable.round] = question.round
                    it[JeopardyQuestionsTable.value] = question.value
                }
            }
        }
    }

    suspend fun updateUserScore(userId: String, points: Int, right: Boolean): JeopardyScoreRow {
        return dbUpsert(
            table = JeopardyScoreTable,
            keys = arrayOf(JeopardyScoreTable.userId),
            onUpdate = listOf(
                JeopardyScoreTable.points to (JeopardyScoreTable.points + if (right) points else -points),
                JeopardyScoreTable.right to (JeopardyScoreTable.right + if (right) 1 else 0),
                JeopardyScoreTable.wrong to (JeopardyScoreTable.wrong + if (right) 0 else 1)
            ),
            insertBody = {
                it[JeopardyScoreTable.userId] = userId
                it[JeopardyScoreTable.points] = if (right) points else -points
                it[JeopardyScoreTable.right] = if (right) 1 else 0
                it[JeopardyScoreTable.wrong] = if (right) 0 else 1
            },
            selectWhere = { JeopardyScoreTable.userId eq userId },
            mapper = { resultRow ->
                JeopardyScoreRow(
                    id = resultRow[JeopardyScoreTable.id],
                    userId = resultRow[JeopardyScoreTable.userId],
                    points = resultRow[JeopardyScoreTable.points],
                    right = resultRow[JeopardyScoreTable.right],
                    wrong = resultRow[JeopardyScoreTable.wrong]
                )
            }
        )
    }
}
