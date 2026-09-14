package com.example

import com.google.gson.annotations.SerializedName
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.config.yaml.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

data class Habit(
    val id: String,
    val title: String,
    val description: String,
    @SerializedName("total_days") val totalDays: Int,
    @SerializedName("current_progress") val currentProgress: Int,
    @SerializedName("last_completed_date") val lastCompletedDate: String?
)

object HabitsTable : Table() {
    val id = varchar("id", 36)
    val title = varchar("title", 255)
    val description = text("description")
    val totalDays = integer("total_days")
    val currentProgress = integer("current_progress")
    val lastCompletedDate = varchar("last_completed_date", 50).nullable()

    override val primaryKey = PrimaryKey(id)
}

fun main() {
    val appConfig = YamlConfig("application.yaml")

    // 2. Extract the variables securely
    // In YAML, if the config can't find it, it throws an error safely,
    // so we make sure to handle null safety if it's missing.
    val dbUrl = appConfig?.property("database.url")?.getString() ?: ""
    val dbUser = appConfig?.property("database.user")?.getString() ?: ""
    val dbPassword = appConfig?.property("database.password")?.getString() ?: ""

    Database.connect(
        url = dbUrl,
        driver = "org.h2.Driver",
        user = dbUser,
        password = dbPassword
    )

    transaction {
        SchemaUtils.create(HabitsTable)
    }
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson {}
        }
        routing {
            post("/api/habits") {
                val newHabit = call.receive<Habit>()
                transaction {
                    HabitsTable.upsert(HabitsTable.id) {
                        it[id] = newHabit.id
                        it[title] = newHabit.title
                        it[description] = newHabit.description
                        it[totalDays] = newHabit.totalDays
                        it[currentProgress] = newHabit.currentProgress
                        it[lastCompletedDate] = newHabit.lastCompletedDate
                    }
                }
                call.respondText("Successfully saved ${newHabit.title} to the database!")
            }

            get("/api/habits") {
                val allHabits = transaction {
                    HabitsTable.selectAll().map { row ->
                        Habit(
                            id = row[HabitsTable.id],
                            title = row[HabitsTable.title],
                            description = row[HabitsTable.description],
                            totalDays = row[HabitsTable.totalDays],
                            currentProgress = row[HabitsTable.currentProgress],
                            lastCompletedDate = row[HabitsTable.lastCompletedDate]
                        )
                    }
                }
                call.respond(allHabits)
            }

            put("/api/habits/{id}") {
                val habitId = call.parameters["id"]
                if (habitId == null) {
                    call.respond("Invalid or missing id")
                    return@put
                }

                val updatedData = call.receive<Habit>()
                transaction {
                    HabitsTable.update({ HabitsTable.id eq habitId }) {
                        it[title] = updatedData.title
                        it[description] = updatedData.description
                        it[totalDays] = updatedData.totalDays
                        it[currentProgress] = updatedData.currentProgress
                        it[lastCompletedDate] = updatedData.lastCompletedDate
                    }
                }

                call.respondText { "Successfully updated ${updatedData.title}!" }
            }
            delete("/api/habits/{id}") {
                val habitId = call.parameters["id"]
                if (habitId == null) {
                    call.respond("Invalid or missing id")
                    return@delete
                }

                val habit = transaction {
                    HabitsTable.deleteWhere { HabitsTable.id eq habitId }
                }

                call.respondText { "Successfully deleted $habit!" }
            }
        }
    }.start(wait = true)
}
