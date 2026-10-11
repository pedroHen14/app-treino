package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.template
import com.pedro.treinoapp.feature.workout.domain.templateExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateWorkoutTemplateUseCaseTest {

    private val repository = FakeWorkoutTemplateRepository(
        listOf(template(1, "Upper", 10), template(2, "Pernas e glúteos", 20)),
    )
    private val validate = ValidateWorkoutTemplateUseCase(repository)
    private val oneExercise = listOf(templateExercise(1))

    @Test
    fun `empty name is rejected`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NameBlank), validate("", oneExercise))
    }

    @Test
    fun `name with only spaces is rejected`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NameBlank), validate("  \t ", oneExercise))
    }

    @Test
    fun `name is trimmed and inner spaces collapsed`() = runTest {
        assertEquals(WorkoutResult.Success("Full body"), validate("  Full   body ", oneExercise))
    }

    @Test
    fun `duplicate name with different case is rejected`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NameAlreadyExists), validate("UPPER", oneExercise))
    }

    @Test
    fun `duplicate name with accented letters in different case is rejected`() = runTest {
        assertEquals(
            WorkoutResult.Failure(WorkoutError.NameAlreadyExists),
            validate("PERNAS E GLÚTEOS", oneExercise),
        )
    }

    @Test
    fun `template can keep its own name when edited`() = runTest {
        assertEquals(WorkoutResult.Success("upper"), validate("upper", oneExercise, excludingId = 1))
    }

    @Test
    fun `template without exercises is rejected`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NoExercises), validate("Lower", emptyList()))
    }

    @Test
    fun `same exercise twice is rejected at the repeated position`() = runTest {
        val exercises = listOf(templateExercise(1), templateExercise(2), templateExercise(1))
        assertEquals(WorkoutResult.Failure(WorkoutError.DuplicateExercise(2)), validate("Lower", exercises))
    }

    @Test
    fun `sets below minimum are rejected`() = runTest {
        val exercises = listOf(templateExercise(1), templateExercise(2, sets = 0))
        assertEquals(WorkoutResult.Failure(WorkoutError.InvalidSets(1)), validate("Lower", exercises))
    }

    @Test
    fun `sets above maximum are rejected`() = runTest {
        val exercises = listOf(templateExercise(1, sets = 21))
        assertEquals(WorkoutResult.Failure(WorkoutError.InvalidSets(0)), validate("Lower", exercises))
    }

    @Test
    fun `sets at the limits are accepted`() = runTest {
        val exercises = listOf(templateExercise(1, sets = 1), templateExercise(2, sets = 20))
        assertEquals(WorkoutResult.Success("Lower"), validate("Lower", exercises))
    }

    @Test
    fun `fixed reps are accepted`() = runTest {
        val exercises = listOf(templateExercise(1, reps = RepRange.fixed(10)))
        assertEquals(WorkoutResult.Success("Lower"), validate("Lower", exercises))
    }

    @Test
    fun `rep range is accepted`() = runTest {
        val exercises = listOf(templateExercise(1, reps = RepRange(8, 12)))
        assertEquals(WorkoutResult.Success("Lower"), validate("Lower", exercises))
    }

    @Test
    fun `rep range with min above max is rejected`() = runTest {
        val exercises = listOf(templateExercise(1, reps = RepRange(12, 8)))
        assertEquals(WorkoutResult.Failure(WorkoutError.InvalidReps(0)), validate("Lower", exercises))
    }

    @Test
    fun `zero reps are rejected`() = runTest {
        val exercises = listOf(templateExercise(1, reps = RepRange(0, 10)))
        assertEquals(WorkoutResult.Failure(WorkoutError.InvalidReps(0)), validate("Lower", exercises))
    }

    @Test
    fun `reps above maximum are rejected`() = runTest {
        val exercises = listOf(templateExercise(1, reps = RepRange(10, 101)))
        assertEquals(WorkoutResult.Failure(WorkoutError.InvalidReps(0)), validate("Lower", exercises))
    }
}
