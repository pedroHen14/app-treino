package com.pedro.treinoapp.debug

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.core.time.weekStart
import dagger.hilt.android.AndroidEntryPoint
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.concurrent.thread

/**
 * Popula o banco com modelos, agenda e doze semanas de histórico fictício, para as capturas
 * da Play Store. Apaga treinos, agenda e sessões existentes; o catálogo de exercícios fica.
 *
 * Disparo (com o app instalado em debug):
 * `adb shell am broadcast -n com.pedro.musclenotes/com.pedro.treinoapp.debug.DemoSeedReceiver`
 * e depois `adb shell am force-stop com.pedro.musclenotes` para reabrir com os dados novos.
 * Com `--ez active true`, também deixa o treino de hoje em andamento, com a primeira série feita.
 */
@AndroidEntryPoint
class DemoSeedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var database: AppDatabase

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        thread {
            try {
                DemoSeed.run(
                    db = database.openHelper.writableDatabase,
                    today = LocalDate.now(),
                    withActiveSession = intent.getBooleanExtra(EXTRA_ACTIVE, false),
                )
                Log.i(TAG, "Seed concluída")
                pending.resultData = "ok"
            } catch (e: Exception) {
                Log.e(TAG, "Seed falhou", e)
                pending.resultData = "erro: ${e.message}"
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "DemoSeed"
        const val EXTRA_ACTIVE = "active"
    }
}

private object DemoSeed {

    private const val HISTORY_WEEKS = 12

    private class Item(
        val exercise: String,
        val sets: Int,
        val repsMin: Int,
        val repsMax: Int,
        val baseKg: Double,
        val stepKg: Double,
    )

    private class Template(val name: String, val days: List<DayOfWeek>, val items: List<Item>)

    private val templates = listOf(
        Template(
            "Peito e tríceps",
            listOf(DayOfWeek.MONDAY),
            listOf(
                Item("Supino reto com barra", 4, 6, 8, 50.0, 5.0),
                Item("Supino inclinado com halteres", 3, 8, 10, 24.0, 2.0),
                Item("Crossover", 3, 10, 12, 15.0, 2.5),
                Item("Tríceps pulley", 3, 10, 12, 25.0, 2.5),
                Item("Tríceps testa", 3, 8, 10, 20.0, 2.5),
            ),
        ),
        Template(
            "Costas e bíceps",
            listOf(DayOfWeek.TUESDAY),
            listOf(
                Item("Puxada frontal", 4, 8, 10, 55.0, 2.5),
                Item("Remada curvada com barra", 4, 6, 8, 50.0, 2.5),
                Item("Remada baixa", 3, 10, 12, 50.0, 5.0),
                Item("Rosca direta com barra", 3, 8, 10, 25.0, 2.5),
                Item("Rosca martelo", 3, 10, 12, 12.0, 1.0),
            ),
        ),
        Template(
            "Pernas",
            listOf(DayOfWeek.THURSDAY),
            listOf(
                Item("Agachamento livre", 4, 6, 8, 80.0, 5.0),
                Item("Leg press 45°", 4, 10, 12, 180.0, 10.0),
                Item("Cadeira extensora", 3, 12, 15, 45.0, 5.0),
                Item("Mesa flexora", 3, 10, 12, 35.0, 2.5),
                Item("Panturrilha em pé", 4, 12, 15, 60.0, 5.0),
            ),
        ),
        Template(
            "Ombros e core",
            listOf(DayOfWeek.FRIDAY),
            listOf(
                Item("Desenvolvimento com halteres", 4, 8, 10, 20.0, 2.0),
                Item("Elevação lateral", 4, 12, 15, 8.0, 1.0),
                Item("Face pull", 3, 12, 15, 20.0, 2.5),
                Item("Encolhimento com halteres", 3, 10, 12, 28.0, 2.0),
                Item("Abdominal na polia", 3, 12, 15, 35.0, 5.0),
            ),
        ),
    )

    /** Anotações por (semana, dia); semana 0 é a mais antiga. */
    private val notes = mapOf(
        (7 to DayOfWeek.THURSDAY) to "Joelho tranquilo no agachamento. Subir o leg press na próxima.",
        (10 to DayOfWeek.FRIDAY) to "Ombro direito um pouco travado no aquecimento, mas passou.",
        (HISTORY_WEEKS to DayOfWeek.MONDAY) to "Primeira vez com 70 kg no supino. Meta: fechar 8 reps.",
    )

    fun run(db: SupportSQLiteDatabase, today: LocalDate, withActiveSession: Boolean) {
        val zone = ZoneId.systemDefault()
        db.beginTransaction()
        try {
            listOf("workout_set", "session_exercise", "workout_session", "weekly_schedule", "template_exercise", "workout_template")
                .forEach { db.execSQL("DELETE FROM $it") }

            val createdAt = today.minusWeeks(HISTORY_WEEKS + 1L).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
            val templateIds = templates.associateWith { template ->
                val id = db.insertRow(
                    "workout_template",
                    "name" to template.name,
                    "created_at" to createdAt,
                    "updated_at" to createdAt,
                )
                template.items.forEachIndexed { position, item ->
                    db.insertRow(
                        "template_exercise",
                        "template_id" to id,
                        "exercise_id" to db.exerciseId(item.exercise),
                        "position" to position,
                        "target_sets" to item.sets,
                        "target_reps_min" to item.repsMin,
                        "target_reps_max" to item.repsMax,
                    )
                }
                id
            }

            DayOfWeek.entries.forEach { day ->
                val template = templates.find { day in it.days }
                db.insertRow("weekly_schedule", "day_of_week" to day.value, "template_id" to template?.let(templateIds::get))
            }

            val firstWeek = today.weekStart().minusWeeks(HISTORY_WEEKS.toLong())
            for (week in 0..HISTORY_WEEKS) {
                val weekStart = firstWeek.plusWeeks(week.toLong())
                templates.forEach { template ->
                    template.days.forEach days@{ day ->
                        val scheduled = weekStart.plusDays(day.value - 1L)
                        if (scheduled >= today) return@days
                        // Uma falta reposta no sábado e uma falta sem reposição, para o histórico parecer real.
                        val missed = (week == 5 && day == DayOfWeek.FRIDAY) || (week == 9 && day == DayOfWeek.THURSDAY)
                        val makeup = week == 9 && day == DayOfWeek.THURSDAY
                        if (missed && !makeup) return@days
                        val date = if (makeup) weekStart.plusDays(5) else scheduled
                        if (date >= today) return@days
                        db.insertSession(
                            template = template,
                            templateId = templateIds.getValue(template),
                            week = week,
                            date = date,
                            makeupFor = scheduled.takeIf { makeup },
                            note = notes[week to day],
                            zone = zone,
                        )
                    }
                }
            }
            if (withActiveSession) {
                templates.find { today.dayOfWeek in it.days }?.let { template ->
                    db.insertSession(
                        template = template,
                        templateId = templateIds.getValue(template),
                        week = HISTORY_WEEKS,
                        date = today,
                        makeupFor = null,
                        note = null,
                        zone = zone,
                        inProgress = true,
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun SupportSQLiteDatabase.insertSession(
        template: Template,
        templateId: Long,
        week: Int,
        date: LocalDate,
        makeupFor: LocalDate?,
        note: String?,
        zone: ZoneId,
        inProgress: Boolean = false,
    ) {
        val start = if (inProgress) {
            Instant.now().minusSeconds(10 * 60)
        } else {
            date.atTime(LocalTime.of(18, 10 + (week * 7) % 40)).atZone(zone).toInstant()
        }
        var clock = start.plusSeconds(4 * 60)
        val sessionId = insertRow(
            "workout_session",
            "template_id" to templateId,
            "template_name" to template.name,
            "date" to date.toEpochDay(),
            "started_at" to start.toEpochMilli(),
            "finished_at" to null,
            "makeup_for_date" to makeupFor?.toEpochDay(),
            "notes" to note,
        )
        template.items.forEachIndexed { position, item ->
            val sessionExerciseId = insertRow(
                "session_exercise",
                "session_id" to sessionId,
                "exercise_id" to exerciseId(item.exercise),
                "position" to position,
                "target_reps_min" to item.repsMin,
                "target_reps_max" to item.repsMax,
            )
            // Dupla progressão em ciclos de três semanas: sobe a carga, repete, sobe as repetições.
            // A defasagem por exercício espalha os recordes, para nem toda sessão ter o mesmo número.
            val cycle = week + position
            val weightGrams = ((item.baseKg + item.stepKg * (cycle / 3)) * 1000).toInt()
            val topReps = if (cycle % 3 < 2) item.repsMin else minOf(item.repsMin + 2, item.repsMax)
            repeat(item.sets) { setIndex ->
                val fatigue = if (setIndex >= item.sets - 1 && (week + position) % 3 != 0) 1 else 0
                clock = clock.plusSeconds(150L + (setIndex + position * 13 + week * 7) % 50)
                // Em andamento: só a primeira série do primeiro exercício está feita.
                val completed = !inProgress || (position == 0 && setIndex == 0)
                insertRow(
                    "workout_set",
                    "session_exercise_id" to sessionExerciseId,
                    "position" to setIndex,
                    "weight_grams" to weightGrams,
                    "reps" to topReps - fatigue,
                    "is_completed" to if (completed) 1 else 0,
                    "completed_at" to clock.toEpochMilli().takeIf { completed },
                )
            }
            clock = clock.plusSeconds(90)
        }
        if (inProgress) return
        update(
            "workout_session",
            SQLiteDatabase.CONFLICT_ABORT,
            ContentValues().apply { put("finished_at", clock.plusSeconds(5 * 60).toEpochMilli()) },
            "id = ?",
            arrayOf(sessionId),
        )
    }

    private fun SupportSQLiteDatabase.exerciseId(name: String): Long =
        query("SELECT id FROM exercise WHERE name = ?", arrayOf(name)).use { cursor ->
            check(cursor.moveToFirst()) { "Exercício não encontrado no catálogo: $name" }
            cursor.getLong(0)
        }

    private fun SupportSQLiteDatabase.insertRow(table: String, vararg values: Pair<String, Any?>): Long {
        val content = ContentValues().apply {
            values.forEach { (key, value) ->
                when (value) {
                    null -> putNull(key)
                    is Int -> put(key, value)
                    is Long -> put(key, value)
                    is String -> put(key, value)
                    else -> error("Tipo não suportado: $key")
                }
            }
        }
        return insert(table, SQLiteDatabase.CONFLICT_ABORT, content)
    }
}
