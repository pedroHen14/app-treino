package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/** Nova série no fim do exercício, com carga e repetições da última série dele, ainda não concluída. */
class AddSetUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, sessionExerciseId: Long): SessionResult<Long> {
        val exercise = repository.getEditable(sessionId)?.findExercise(sessionExerciseId)
            ?: return SessionResult.Failure(SessionError.NotFound)
        if (exercise.sets.size >= WorkoutSet.MAX_SETS) return SessionResult.Failure(SessionError.TooManySets)

        val last = exercise.sets.lastOrNull()
        val set = WorkoutSet(weightGrams = last?.weightGrams, reps = last?.reps)
        return SessionResult.Success(repository.addSet(sessionExerciseId, set))
    }
}
