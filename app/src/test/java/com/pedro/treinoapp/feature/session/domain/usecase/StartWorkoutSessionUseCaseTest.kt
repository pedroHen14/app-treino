package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.NOW
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.completedSet
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.session.domain.sessionExercise
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.templateExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartWorkoutSessionUseCaseTest {

    private val upper = WorkoutTemplate(
        id = 1,
        name = "Upper",
        exercises = listOf(templateExercise(10, sets = 2, reps = RepRange(6, 8)), templateExercise(20, sets = 3)),
    )
    private val templates = FakeWorkoutTemplateRepository(listOf(upper))

    private fun startWith(repository: FakeSessionRepository) =
        StartWorkoutSessionUseCase(repository, templates, BuildPrefilledSetsUseCase(repository), fixedClock)

    @Test
    fun `session gets template exercises with target sets and reps`() = runTest {
        val repository = FakeSessionRepository()

        val id = (startWith(repository)(templateId = 1) as SessionResult.Success).value

        val session = repository.get(id)!!
        assertEquals(1L, session.templateId)
        assertEquals("Upper", session.templateName)
        assertEquals(TODAY, session.date)
        assertEquals(NOW, session.startedAt)
        assertTrue(session.isActive)
        assertEquals(listOf(10L, 20L), session.exercises.map { it.exerciseId })
        assertEquals(listOf(RepRange(6, 8), RepRange(8, 12)), session.exercises.map { it.targetReps })
        assertEquals(listOf(2, 3), session.exercises.map { it.sets.size })
    }

    @Test
    fun `sets are prefilled from last execution and empty without history`() = runTest {
        val repository = FakeSessionRepository(
            listOf(session(1, TODAY.minusDays(3), exercises = arrayOf(sessionExercise(1, 10, completedSet(1, 80_000, 6))))),
        )

        val id = (startWith(repository)(templateId = 1) as SessionResult.Success).value

        val session = repository.get(id)!!
        assertEquals(
            List(2) { WorkoutSet(weightGrams = 80_000, reps = 6) },
            session.exercises[0].sets.map { it.copy(id = 0) },
        )
        assertTrue(session.exercises[1].sets.all { it.weightGrams == null && it.reps == null })
    }

    @Test
    fun `empty session has no template and no exercises`() = runTest {
        val repository = FakeSessionRepository()

        val id = (startWith(repository)(templateId = null) as SessionResult.Success).value

        val session = repository.get(id)!!
        assertNull(session.templateId)
        assertNull(session.templateName)
        assertTrue(session.exercises.isEmpty())
    }

    @Test
    fun `only one session can be active`() = runTest {
        val repository = FakeSessionRepository(listOf(session(1, finished = false)))

        assertEquals(SessionResult.Failure(SessionError.AlreadyActive), startWith(repository)(templateId = 1))
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `unknown template fails`() = runTest {
        assertEquals(SessionResult.Failure(SessionError.NotFound), startWith(FakeSessionRepository())(templateId = 99))
    }

    @Test
    fun `makeup date is linked to the session`() = runTest {
        val repository = FakeSessionRepository()
        val monday = TODAY.minusDays(3)

        val id = (startWith(repository)(templateId = 1, makeupForDate = monday) as SessionResult.Success).value

        assertEquals(monday, repository.get(id)!!.makeupForDate)
    }

    @Test
    fun `advanced date is linked to the session`() = runTest {
        val repository = FakeSessionRepository()
        val friday = TODAY.plusDays(1)

        val id = (startWith(repository)(templateId = 1, makeupForDate = friday) as SessionResult.Success).value

        assertEquals(friday, repository.get(id)!!.makeupForDate)
    }

    @Test
    fun `linked date must be another day of the current week`() = runTest {
        val start = startWith(FakeSessionRepository())
        val invalid = SessionResult.Failure(SessionError.InvalidMakeupDate)

        assertEquals(invalid, start(templateId = 1, makeupForDate = TODAY))
        assertEquals(invalid, start(templateId = 1, makeupForDate = TODAY.minusDays(4)))
        assertEquals(invalid, start(templateId = 1, makeupForDate = TODAY.plusDays(4)))
        assertEquals(invalid, start(templateId = null, makeupForDate = TODAY.minusDays(1)))
        assertEquals(invalid, start(templateId = null, makeupForDate = TODAY.plusDays(1)))
    }
}
