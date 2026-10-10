package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseFilter
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveExercisesUseCaseTest {

    private val repository = FakeExerciseRepository(
        listOf(
            Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false),
            Exercise(id = 2, name = "Elevação lateral", muscleGroup = MuscleGroup.SHOULDERS, isCustom = false),
            Exercise(id = 3, name = "Agachamento livre", muscleGroup = MuscleGroup.QUADRICEPS, isCustom = false),
            Exercise(id = 4, name = "Supino inclinado", muscleGroup = MuscleGroup.CHEST, isCustom = true),
            Exercise(
                id = 5,
                name = "Crucifixo",
                muscleGroup = MuscleGroup.CHEST,
                isCustom = false,
                isArchived = true,
            ),
            Exercise(id = 6, name = "Ênfase no glúteo", muscleGroup = MuscleGroup.GLUTES, isCustom = true),
        ),
    )
    private val observe = ObserveExercisesUseCase(repository)

    private suspend fun namesFor(filter: ExerciseFilter) = observe(flowOf(filter)).first().map { it.name }

    @Test
    fun `archived exercises are hidden by default`() = runTest {
        assertEquals(false, namesFor(ExerciseFilter()).contains("Crucifixo"))
    }

    @Test
    fun `archived exercises are shown when requested`() = runTest {
        assertEquals(true, namesFor(ExerciseFilter(includeArchived = true)).contains("Crucifixo"))
    }

    @Test
    fun `list is sorted alphabetically with accented letters in place`() = runTest {
        assertEquals(
            listOf("Agachamento livre", "Elevação lateral", "Ênfase no glúteo", "Supino inclinado", "Supino reto"),
            namesFor(ExerciseFilter()),
        )
    }

    @Test
    fun `search ignores case`() = runTest {
        assertEquals(listOf("Supino inclinado", "Supino reto"), namesFor(ExerciseFilter(query = "SUPINO")))
    }

    @Test
    fun `search ignores accents`() = runTest {
        assertEquals(listOf("Elevação lateral"), namesFor(ExerciseFilter(query = "elevacao")))
    }

    @Test
    fun `search matches any part of the name`() = runTest {
        assertEquals(listOf("Supino reto"), namesFor(ExerciseFilter(query = " reto ")))
    }

    @Test
    fun `filter by muscle group`() = runTest {
        assertEquals(
            listOf("Supino inclinado", "Supino reto"),
            namesFor(ExerciseFilter(muscleGroup = MuscleGroup.CHEST)),
        )
    }

    @Test
    fun `search and muscle group combine`() = runTest {
        assertEquals(
            listOf("Supino reto"),
            namesFor(ExerciseFilter(query = "reto", muscleGroup = MuscleGroup.CHEST)),
        )
    }

    @Test
    fun `list updates when an exercise is added`() = runTest {
        val flow = observe(flowOf(ExerciseFilter(muscleGroup = MuscleGroup.BICEPS)))
        assertEquals(emptyList<Exercise>(), flow.first())

        repository.insert(Exercise(name = "Rosca direta", muscleGroup = MuscleGroup.BICEPS, isCustom = true))

        assertEquals(listOf("Rosca direta"), flow.first().map { it.name })
    }
}
