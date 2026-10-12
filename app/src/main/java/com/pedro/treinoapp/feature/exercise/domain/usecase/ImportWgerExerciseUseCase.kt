package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.core.text.toNameKey
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Salva no catálogo local um exercício do wger, com o nome e o grupo escolhidos pelo usuário
 * (o nome pode ter vindo em inglês e o grupo pode não ter correspondência).
 *
 * Bloqueia se o mesmo exercício do wger já foi importado ou se o nome já existe no catálogo,
 * devolvendo o exercício existente para ser sugerido. O importado fica como personalizado:
 * pode ser editado, arquivado e excluído.
 *
 * @return o id do exercício criado.
 */
class ImportWgerExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
    private val validateName: ValidateExerciseNameUseCase,
) {
    suspend operator fun invoke(
        exercise: WgerExercise,
        name: String,
        muscleGroup: MuscleGroup?,
    ): ExerciseResult<Long> {
        val catalog = repository.getAll()
        catalog.find { it.wgerId == exercise.wgerId }?.let { return alreadyInCatalog(it) }

        val validName = when (val validation = validateName(name)) {
            is ExerciseResult.Success -> validation.value
            is ExerciseResult.Failure -> {
                if (validation.error == ExerciseError.NameAlreadyExists) {
                    val key = name.toNameKey()
                    catalog.find { it.name.toNameKey() == key }?.let { return alreadyInCatalog(it) }
                }
                return validation
            }
        }
        if (muscleGroup == null) return ExerciseResult.Failure(ExerciseError.MuscleGroupRequired)

        val id = repository.insert(
            Exercise(
                name = validName,
                muscleGroup = muscleGroup,
                isCustom = true,
                description = exercise.description,
                wgerId = exercise.wgerId,
            ),
        )
        return ExerciseResult.Success(id)
    }

    private fun alreadyInCatalog(existing: Exercise) =
        ExerciseResult.Failure(ExerciseError.AlreadyInCatalog(existing))
}
