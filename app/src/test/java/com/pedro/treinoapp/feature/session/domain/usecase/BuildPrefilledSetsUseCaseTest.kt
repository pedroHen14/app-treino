package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.completedSet
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.session.domain.sessionExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildPrefilledSetsUseCaseTest {

    @Test
    fun `without previous execution sets come empty`() = runTest {
        val build = BuildPrefilledSetsUseCase(FakeSessionRepository())

        assertEquals(List(3) { WorkoutSet() }, build(exerciseId = 10, count = 3))
    }

    @Test
    fun `sets copy weight and reps of the same position in the last execution`() = runTest {
        val repository = FakeSessionRepository(
            listOf(
                session(1, TODAY.minusDays(7), exercises = arrayOf(sessionExercise(1, 10, completedSet(1, 60_000, 8)))),
                session(
                    2,
                    TODAY.minusDays(3),
                    exercises = arrayOf(
                        sessionExercise(2, 10, completedSet(2, 70_000, 10), completedSet(3, 72_500, 8)),
                    ),
                ),
            ),
        )
        val build = BuildPrefilledSetsUseCase(repository)

        assertEquals(
            listOf(WorkoutSet(weightGrams = 70_000, reps = 10), WorkoutSet(weightGrams = 72_500, reps = 8)),
            build(exerciseId = 10, count = 2),
        )
    }

    @Test
    fun `extra sets repeat the last set of the previous execution`() = runTest {
        val repository = FakeSessionRepository(
            listOf(session(1, TODAY.minusDays(2), exercises = arrayOf(sessionExercise(1, 10, completedSet(1, 50_000, 12))))),
        )
        val build = BuildPrefilledSetsUseCase(repository)

        assertEquals(List(3) { WorkoutSet(weightGrams = 50_000, reps = 12) }, build(exerciseId = 10, count = 3))
    }

    @Test
    fun `unfinished session is not used as previous execution`() = runTest {
        val repository = FakeSessionRepository(
            listOf(
                session(1, TODAY, finished = false, exercises = arrayOf(sessionExercise(1, 10, completedSet(1, 50_000, 12)))),
            ),
        )

        assertEquals(List(2) { WorkoutSet() }, BuildPrefilledSetsUseCase(repository)(exerciseId = 10, count = 2))
    }

    @Test
    fun `without count repeats the number of sets of the last execution`() = runTest {
        val repository = FakeSessionRepository(
            listOf(
                session(
                    1,
                    TODAY.minusDays(1),
                    exercises = arrayOf(sessionExercise(1, 10, completedSet(1, 40_000, 10), completedSet(2, 40_000, 9))),
                ),
            ),
        )
        val build = BuildPrefilledSetsUseCase(repository)

        assertEquals(2, build(exerciseId = 10).size)
        assertEquals(3, build(exerciseId = 99).size)
    }
}
