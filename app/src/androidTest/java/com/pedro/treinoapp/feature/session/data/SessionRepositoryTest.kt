package com.pedro.treinoapp.feature.session.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseEntity
import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase
    private lateinit var repository: SessionRepositoryImpl
    private val exerciseIds = mutableListOf<Long>()

    private val today = LocalDate.of(2026, 10, 15)
    private val start = Instant.parse("2026-10-15T18:00:00Z")

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = SessionRepositoryImpl(database.sessionDao())
        listOf("Supino reto", "Remada curvada", "Desenvolvimento").forEach { name ->
            exerciseIds += database.exerciseDao().insert(ExerciseEntity(name = name, muscleGroup = "CHEST", isCustom = true))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun create(
        date: LocalDate = today,
        templateId: Long? = null,
        makeupForDate: LocalDate? = null,
        vararg exercises: NewSessionExercise,
    ) = repository.create(
        NewWorkoutSession(
            templateId = templateId,
            templateName = null,
            date = date,
            startedAt = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
            makeupForDate = makeupForDate,
            exercises = exercises.toList(),
        ),
    )

    @Test
    fun createdSessionKeepsOrderTargetsAndEmptySets() = runBlocking {
        val id = create(
            exercises = arrayOf(
                NewSessionExercise(exerciseIds[2], RepRange(8, 12), listOf(WorkoutSet(), WorkoutSet())),
                NewSessionExercise(exerciseIds[0], null, listOf(WorkoutSet(weightGrams = 60_000, reps = 10))),
            ),
        )

        val session = repository.observeActive().first()!!
        assertEquals(id, session.id)
        assertEquals(listOf("Desenvolvimento", "Supino reto"), session.exercises.map { it.exerciseName })
        assertEquals(listOf(RepRange(8, 12), null), session.exercises.map { it.targetReps })
        assertEquals(listOf(null, null), session.exercises[0].sets.map { it.weightGrams })
        assertEquals(60_000, session.exercises[1].sets.single().weightGrams)
        assertEquals(id, repository.getActiveId())
    }

    @Test
    fun exerciseWithoutSetsStillAppears() = runBlocking {
        val id = create(exercises = arrayOf(NewSessionExercise(exerciseIds[0], sets = emptyList())))

        val exercise = repository.get(id)!!.exercises.single()
        assertTrue(exercise.sets.isEmpty())
    }

    @Test
    fun editsAppendAtTheEndAndReplaceKeepsPosition() = runBlocking {
        val id = create(exercises = arrayOf(NewSessionExercise(exerciseIds[0], sets = listOf(WorkoutSet()))))
        val first = repository.get(id)!!.exercises.single()

        val setId = repository.addSet(first.id, WorkoutSet(weightGrams = 20_000, reps = 12))
        repository.updateSet(first.sets.single().id, 22_500, 10)
        repository.setCompleted(setId, start)
        repository.addExercise(id, NewSessionExercise(exerciseIds[1], sets = listOf(WorkoutSet())))
        repository.replaceExercise(first.id, exerciseIds[2], listOf(WorkoutSet(weightGrams = 5_000, reps = 15)))

        val session = repository.get(id)!!
        assertEquals(listOf("Desenvolvimento", "Remada curvada"), session.exercises.map { it.exerciseName })
        assertEquals(listOf(5_000), session.exercises[0].sets.map { it.weightGrams })
    }

    @Test
    fun finishDropsPendingSetsAndEmptyExercises() = runBlocking {
        val id = create(
            exercises = arrayOf(
                NewSessionExercise(exerciseIds[0], sets = listOf(WorkoutSet(weightGrams = 60_000, reps = 10), WorkoutSet())),
                NewSessionExercise(exerciseIds[1], sets = listOf(WorkoutSet())),
            ),
        )
        val doneSetId = repository.get(id)!!.exercises[0].sets[0].id
        repository.setCompleted(doneSetId, start)

        repository.finish(id, start.plusSeconds(3600))

        val saved = repository.get(id)!!
        assertEquals(start.plusSeconds(3600), saved.finishedAt)
        assertEquals(listOf(exerciseIds[0]), saved.exercises.map { it.exerciseId })
        assertEquals(listOf(doneSetId), saved.exercises.single().sets.map { it.id })
        assertNull(repository.getActiveId())
        assertNull(repository.observeActive().first())
    }

    @Test
    fun lastCompletedSetsComeFromTheMostRecentFinishedSession() = runBlocking {
        suspend fun finishedWith(date: LocalDate, weight: Int) {
            val id = create(date, exercises = arrayOf(NewSessionExercise(exerciseIds[0], sets = listOf(WorkoutSet(weightGrams = weight, reps = 8)))))
            repository.setCompleted(repository.get(id)!!.exercises.single().sets.single().id, start)
            repository.finish(id, start)
        }
        finishedWith(today.minusDays(7), 50_000)
        finishedWith(today.minusDays(3), 55_000)
        val active = create(exercises = arrayOf(NewSessionExercise(exerciseIds[0], sets = listOf(WorkoutSet(weightGrams = 90_000, reps = 1)))))
        repository.setCompleted(repository.get(active)!!.exercises.single().sets.single().id, start)

        assertEquals(listOf(55_000 to 8), repository.getLastCompletedSets(exerciseIds[0]).map { it.weightGrams to it.reps })
        assertTrue(repository.getLastCompletedSets(exerciseIds[1]).isEmpty())
    }

    @Test
    fun sessionDaysAndDeletion() = runBlocking {
        val monday = today.minusDays(3)
        create(monday, makeupForDate = null)
        val makeup = create(today.minusDays(1), makeupForDate = monday)

        assertEquals(
            listOf(SessionDay(monday, null, null), SessionDay(today.minusDays(1), null, monday)),
            repository.getSessionDays(monday, today).sortedBy { it.date },
        )

        repository.delete(makeup)
        assertEquals(1, repository.getSessionDays(monday, today).size)
    }

    @Test
    fun finishedSessionDaysOnlyIncludeFinishedSessionsInTheRangeInOrder() = runBlocking {
        val monday = today.minusDays(3)
        val sunday = monday.plusDays(6)
        suspend fun finished(date: LocalDate, makeupForDate: LocalDate? = null) {
            repository.finish(create(date, makeupForDate = makeupForDate), start)
        }
        finished(today)
        finished(monday.minusDays(1))
        finished(sunday.plusDays(1))
        finished(monday.plusDays(2), makeupForDate = monday.plusDays(1))
        finished(sunday)
        create(today)

        assertEquals(
            listOf(
                SessionDay(monday.plusDays(2), null, monday.plusDays(1)),
                SessionDay(today, null, null),
                SessionDay(sunday, null, null),
            ),
            repository.observeFinishedSessionDays(monday, sunday).first(),
        )
    }
}
