package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.core.text.normalizeName
import com.pedro.treinoapp.core.text.toNameKey
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Nome obrigatório e único sem diferenciar maiúsculas e minúsculas.
 * A comparação é feita aqui porque o NOCASE do SQLite só cobre ASCII ("ELEVAÇÃO" e "elevação"
 * seriam nomes diferentes para o índice). Arquivados também contam: o nome continua ocupado.
 *
 * @return o nome normalizado, pronto para salvar.
 */
class ValidateExerciseNameUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(rawName: String, excludingId: Long? = null): ExerciseResult<String> {
        val name = rawName.normalizeName()
        if (name.isEmpty()) return ExerciseResult.Failure(ExerciseError.NameBlank)

        val key = name.toNameKey()
        val taken = repository.getAll().any { it.id != excludingId && it.name.toNameKey() == key }
        if (taken) return ExerciseResult.Failure(ExerciseError.NameAlreadyExists)

        return ExerciseResult.Success(name)
    }
}
