package com.pedro.treinoapp.feature.history.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseEntity
import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.session.data.SessionRepositoryImpl
import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class HistoryRepositoryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var database: AppDatabase
    private lateinit var sessions: SessionRepositoryImpl
    private lateinit var repository: HistoryRepositoryImpl
    private val exerciseIds = mutableListOf<Long>()

    private val day1 = LocalDate.of(2026, 10, 5)
    private val day2 = LocalDate.of(2026, 10, 8)

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        sessions = SessionRepositoryImpl(database.sessionDao())
        repository = HistoryRepositoryImpl(database.historyDao())
        listOf("Supino reto", "Agachamento livre").forEach { name ->
            exerciseIds += database.exerciseDao().insert(ExerciseEntity(name = name, muscleGroup = "CHEST", isCustom = true))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun done(weightGrams: Int, reps: Int) =
        WorkoutSet(weightGrams = weightGrams, reps = reps, isCompleted = true, completedAt = day1.atStartOfDay().toInstant(ZoneOffset.UTC))

    /** Cria a sessão das 8h do dia e, com [finish], finaliza às 9h (séries pendentes são descartadas). */
    private suspend fun session(date: LocalDate, vararg exercises: NewSessionExercise, finish: Boolean = true): Long {
        val id = sessions.create(
            NewWorkoutSession(
                templateId = null,
                templateName = "Upper",
                date = date,
                startedAt = date.atTime(8, 0).toInstant(ZoneOffset.UTC),
                exercises = exercises.toList(),
            ),
        )
        if (finish) sessions.finish(id, date.atTime(9, 0).toInstant(ZoneOffset.UTC))
        return id
    }

    @Test
    fun onlyFinishedSessionsAppearNewestFirstWithExerciseCount() = runBlocking {
        val first = session(
            day1,
            NewSessionExercise(exerciseIds[0], sets = listOf(done(60_000, 10))),
            NewSessionExercise(exerciseIds[1], sets = listOf(done(100_000, 5))),
        )
        val second = session(day2, NewSessionExercise(exerciseIds[0], sets = listOf(done(62_500, 8))))
        session(day2, NewSessionExercise(exerciseIds[0], sets = listOf(done(70_000, 3))), finish = false)

        val history = repository.observeFinishedSessions().first()

        assertEquals(listOf(second, first), history.map { it.id })
        assertEquals(listOf(1, 2), history.map { it.exerciseCount })
        assertEquals(Duration.ofHours(1), Duration.between(history[1].startedAt, history[1].finishedAt))
    }

    @Test
    fun completedSetsComeInChronologicalOrderAndSkipTheActiveSession() = runBlocking {
        val later = session(day2, NewSessionExercise(exerciseIds[0], sets = listOf(done(62_500, 8), done(62_500, 7))))
        val earlier = session(day1, NewSessionExercise(exerciseIds[0], sets = listOf(done(60_000, 10), WorkoutSet())))
        val active = session(day2, NewSessionExercise(exerciseIds[0], sets = listOf(done(70_000, 3))), finish = false)

        val all = repository.observeCompletedSets().first()
        assertEquals(listOf(earlier, later, later), all.map { it.sessionId })
        assertEquals(listOf(60_000, 62_500, 62_500), all.map { it.weightGrams })
        assertEquals(listOf(10, 8, 7), all.map { it.reps })

        val withActive = repository.observeCompletedSets(listOf(exerciseIds[0]), includeSessionId = active).first()
        assertEquals(listOf(earlier, later, later, active), withActive.map { it.sessionId })
        assertTrue(repository.observeCompletedSets(listOf(exerciseIds[1])).first().isEmpty())
    }

    @Test
    fun exercisesWithHistoryAreCountedPerSession() = runBlocking {
        session(day1, NewSessionExercise(exerciseIds[0], sets = listOf(done(60_000, 10), done(60_000, 9))))
        session(day2, NewSessionExercise(exerciseIds[0], sets = listOf(done(62_500, 8))))
        session(day1, NewSessionExercise(exerciseIds[1], sets = listOf(done(100_000, 5))))

        assertEquals(
            listOf(
                ExerciseWithHistory(exerciseIds[0], "Supino reto", day2, sessionCount = 2),
                ExerciseWithHistory(exerciseIds[1], "Agachamento livre", day1, sessionCount = 1),
            ),
            repository.observeExercisesWithHistory().first(),
        )
    }

    @Test
    fun deletingASessionRemovesItsSetsFromHistory() = runBlocking {
        val first = session(day1, NewSessionExercise(exerciseIds[0], sets = listOf(done(60_000, 10))))
        val second = session(day2, NewSessionExercise(exerciseIds[0], sets = listOf(done(62_500, 8))))

        sessions.delete(first)

        assertEquals(listOf(second), repository.observeFinishedSessions().first().map { it.id })
        assertEquals(listOf(second), repository.observeCompletedSets().first().map { it.sessionId })
    }

    @Test
    fun savingAnEditCompletesFilledSetsAndDropsEmptyOnes() = runBlocking {
        val id = session(day1, NewSessionExercise(exerciseIds[0], sets = listOf(done(60_000, 10))))
        val exercise = sessions.get(id)!!.exercises.single()
        sessions.addSet(exercise.id, WorkoutSet(weightGrams = 60_000, reps = 9))
        sessions.addSet(exercise.id, WorkoutSet())
        sessions.addExercise(id, NewSessionExercise(exerciseIds[1], sets = listOf(WorkoutSet())))

        sessions.saveEdit(id, completedAt = day1.atTime(9, 0).toInstant(ZoneOffset.UTC))

        val saved = sessions.get(id)!!
        assertEquals(listOf(exerciseIds[0]), saved.exercises.map { it.exerciseId })
        assertEquals(listOf(10, 9), saved.exercises.single().sets.map { it.reps })
        assertTrue(saved.exercises.single().sets.all { it.isCompleted })
        assertEquals(listOf(10, 9), repository.observeCompletedSets().first().map { it.reps })
    }

    @Test
    fun sessionExerciseIdsFollowTheSessionOrder() = runBlocking {
        val id = session(
            day1,
            NewSessionExercise(exerciseIds[1], sets = listOf(done(100_000, 5))),
            NewSessionExercise(exerciseIds[0], sets = listOf(done(60_000, 10))),
        )

        assertEquals(listOf(exerciseIds[1], exerciseIds[0]), repository.observeSessionExerciseIds(id).first())
    }
}
