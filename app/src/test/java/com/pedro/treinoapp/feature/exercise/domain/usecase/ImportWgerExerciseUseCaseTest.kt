package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportWgerExerciseUseCaseTest {

    private val benchPress = Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false)
    private val archivedRow = Exercise(
        id = 2,
        name = "Remada curvada",
        muscleGroup = MuscleGroup.BACK,
        isCustom = true,
        isArchived = true,
    )
    private val imported = Exercise(
        id = 3,
        name = "Rosca Scott",
        muscleGroup = MuscleGroup.BICEPS,
        isCustom = true,
        wgerId = 80,
    )
    private val repository = FakeExerciseRepository(listOf(benchPress, archivedRow, imported))
    private val import = ImportWgerExerciseUseCase(repository, ValidateExerciseNameUseCase(repository))

    private val dumbbellPress = WgerExercise(
        wgerId = 75,
        name = "Supino com halteres",
        isNameInPortuguese = true,
        description = "Deite no banco.",
        muscleGroup = MuscleGroup.CHEST,
    )

    @Test
    fun `saves as custom exercise with description and wger id`() = runTest {
        val result = import(dumbbellPress, dumbbellPress.name, MuscleGroup.CHEST)

        val id = (result as ExerciseResult.Success).value
        assertEquals(
            Exercise(
                id = id,
                name = "Supino com halteres",
                muscleGroup = MuscleGroup.CHEST,
                isCustom = true,
                description = "Deite no banco.",
                wgerId = 75,
            ),
            repository.getById(id),
        )
    }

    @Test
    fun `saves the name and group chosen by the user`() = runTest {
        val english = dumbbellPress.copy(name = "Cable Crossover", isNameInPortuguese = false, muscleGroup = null)

        val result = import(english, "  Crucifixo   no cabo ", MuscleGroup.CHEST)

        val saved = repository.getById((result as ExerciseResult.Success).value)!!
        assertEquals("Crucifixo no cabo", saved.name)
        assertEquals(MuscleGroup.CHEST, saved.muscleGroup)
    }

    @Test
    fun `blocks existing name ignoring case and suggests the existing exercise`() = runTest {
        val result = import(dumbbellPress, "SUPINO RETO", MuscleGroup.CHEST)

        assertEquals(ExerciseResult.Failure(ExerciseError.AlreadyInCatalog(benchPress)), result)
        assertEquals(3, repository.current.size)
    }

    @Test
    fun `archived exercise also blocks the name`() = runTest {
        val result = import(dumbbellPress, "remada curvada", MuscleGroup.BACK)

        assertEquals(ExerciseResult.Failure(ExerciseError.AlreadyInCatalog(archivedRow)), result)
    }

    @Test
    fun `blocks exercise already imported from wger even with another name`() = runTest {
        val sameWgerExercise = dumbbellPress.copy(wgerId = 80)

        val result = import(sameWgerExercise, "Rosca no banco Scott", MuscleGroup.BICEPS)

        assertEquals(ExerciseResult.Failure(ExerciseError.AlreadyInCatalog(imported)), result)
        assertEquals(3, repository.current.size)
    }

    @Test
    fun `requires a name`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameBlank), import(dumbbellPress, "  ", MuscleGroup.CHEST))
    }

    @Test
    fun `requires a muscle group`() = runTest {
        val result = import(dumbbellPress, dumbbellPress.name, null)

        assertEquals(ExerciseResult.Failure(ExerciseError.MuscleGroupRequired), result)
        assertEquals(3, repository.current.size)
    }
}
