package com.pedro.treinoapp.feature.workout.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseEntity
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class WorkoutTemplateRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase
    private lateinit var templates: WorkoutTemplateRepositoryImpl
    private lateinit var schedule: WeeklyScheduleRepositoryImpl
    private val exerciseIds = mutableListOf<Long>()

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val clock = Clock.fixed(Instant.parse("2026-10-11T10:00:00Z"), ZoneOffset.UTC)
        templates = WorkoutTemplateRepositoryImpl(database.workoutTemplateDao(), clock)
        schedule = WeeklyScheduleRepositoryImpl(database.weeklyScheduleDao())
        listOf("Supino reto", "Remada curvada", "Desenvolvimento").forEach { name ->
            exerciseIds += database.exerciseDao().insert(ExerciseEntity(name = name, muscleGroup = "CHEST", isCustom = true))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun exercise(index: Int, reps: RepRange = RepRange(8, 12)) =
        TemplateExercise(exerciseIds[index], "", targetSets = 3, targetReps = reps)

    @Test
    fun savedTemplateKeepsOrderTargetsAndExerciseNames() = runBlocking {
        val id = templates.insert(
            WorkoutTemplate(name = "Upper", exercises = listOf(exercise(2, RepRange.fixed(10)), exercise(0))),
        )

        val saved = templates.getById(id)!!
        assertEquals(listOf("Desenvolvimento", "Supino reto"), saved.exercises.map { it.exerciseName })
        assertEquals(listOf(RepRange.fixed(10), RepRange(8, 12)), saved.exercises.map { it.targetReps })
        assertEquals(2, templates.getSummaries().single().exerciseCount)
    }

    @Test
    fun updateReplacesExercisesWithoutTouchingRecordedSessions() = runBlocking {
        val id = templates.insert(WorkoutTemplate(name = "Upper", exercises = listOf(exercise(0), exercise(1))))
        val db = database.openHelper.writableDatabase
        db.execSQL(
            "INSERT INTO workout_session (id, template_id, template_name, date, started_at) VALUES (1, ?, 'Upper', 20372, 0)",
            arrayOf(id),
        )
        db.execSQL("INSERT INTO session_exercise (session_id, exercise_id, position) VALUES (1, ?, 0)", arrayOf(exerciseIds[0]))
        db.execSQL("INSERT INTO session_exercise (session_id, exercise_id, position) VALUES (1, ?, 1)", arrayOf(exerciseIds[1]))

        templates.update(WorkoutTemplate(id = id, name = "Upper A", exercises = listOf(exercise(2))))

        assertEquals(listOf(exerciseIds[2]), templates.getById(id)!!.exercises.map { it.exerciseId })
        db.query("SELECT template_name FROM workout_session WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("Upper", it.getString(0))
        }
        db.query("SELECT exercise_id FROM session_exercise WHERE session_id = 1 ORDER BY position").use {
            val ids = generateSequence { if (it.moveToNext()) it.getLong(0) else null }.toList()
            assertEquals(exerciseIds.take(2), ids)
        }
    }

    @Test
    fun clearingAndDeletingTemplateTurnsItsDaysIntoRest() = runBlocking {
        val upper = templates.insert(WorkoutTemplate(name = "Upper", exercises = listOf(exercise(0))))
        val lower = templates.insert(WorkoutTemplate(name = "Lower", exercises = listOf(exercise(1))))
        schedule.setDay(DayOfWeek.MONDAY, upper)
        schedule.setDay(DayOfWeek.TUESDAY, lower)
        schedule.setDay(DayOfWeek.THURSDAY, upper)

        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), schedule.getDaysFor(upper))
        schedule.clearTemplate(upper)
        templates.delete(upper)

        assertNull(templates.getById(upper))
        assertEquals(
            mapOf(DayOfWeek.MONDAY to null, DayOfWeek.TUESDAY to lower, DayOfWeek.THURSDAY to null),
            schedule.observe().first(),
        )
    }
}
