package com.slackcat.app.modules.jeopardy

import com.slackcat.app.SlackcatAppGraph.slackcatNetworkClient
import com.slackcat.app.modules.kudos.dbQuery
import com.slackcat.client.SlackcatNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import kotlin.random.Random

class JeopardyDAO {
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
        val userId = text("user_id")
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

    suspend fun getJeopardyScore(userId: String): JeopardyScoreRow {
        return dbQuery {
            val user = JeopardyScoreTable.select { JeopardyScoreTable.userId eq userId }.single()
            return@dbQuery JeopardyScoreRow(
                    id = user[JeopardyScoreTable.id],
                    userId = user[JeopardyScoreTable.userId],
                    points = user[JeopardyScoreTable.points],
                    right = user[JeopardyScoreTable.right],
                    wrong = user[JeopardyScoreTable.wrong]
                )
        }
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
        val jsonGistFile = slackcatNetworkClient.fetchString(gistUrl)
            .toString()
        return transaction {
            val jsonString = jsonGistFile.substringAfter("Success(").substringBeforeLast(")")
            val json = Json { ignoreUnknownKeys = true }
            val jeopardyQuestions: List<JeopardyQuestionRow> = json.decodeFromString(jsonString)
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
        return jeopardyDbQuery {
            val existingUser = JeopardyScoreTable.select { JeopardyScoreTable.userId eq userId }.singleOrNull()
            if (existingUser == null) {
                // User does not exist, insert new user
                JeopardyScoreTable.insert {
                    it[JeopardyScoreTable.userId] = userId
                    it[JeopardyScoreTable.points] = points
                    it[JeopardyScoreTable.right] = if (right) 1 else 0
                    it[JeopardyScoreTable.wrong] = if (right) 0 else 1
                }
            } else {
                // User exists, increment points and right, wrong numbers
                JeopardyScoreTable.update({ JeopardyScoreTable.userId eq userId }) {
                    with(SqlExpressionBuilder) {
                        it.update(JeopardyScoreTable.points, JeopardyScoreTable.points + if (right) points else (0-points))
                        it.update(JeopardyScoreTable.right, JeopardyScoreTable.right + if (right) 1 else 0)
                        it.update(JeopardyScoreTable.wrong, JeopardyScoreTable.wrong + if (right) 0 else 1)
                    }
                }
            }

            val resultRow = JeopardyScoreTable.select { JeopardyScoreTable.userId eq userId }.single()
            return@jeopardyDbQuery JeopardyScoreRow(
                id = resultRow[JeopardyScoreTable.id],
                userId = resultRow[JeopardyScoreTable.userId],
                points = resultRow[JeopardyScoreTable.points],
                right = resultRow[JeopardyScoreTable.right],
                wrong = resultRow[JeopardyScoreTable.wrong]
            )
        }
    }
}

suspend fun <T> jeopardyDbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
