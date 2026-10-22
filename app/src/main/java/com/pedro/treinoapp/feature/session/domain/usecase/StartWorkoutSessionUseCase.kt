package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.isLinkableDay
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Cria a sessão de hoje a partir de um modelo, ou vazia com `templateId` nulo. Só pode haver
 * uma sessão em andamento. `makeupForDate`, quando informado, é o dia reposto (anterior a hoje) ou
 * adiantado (posterior) e precisa estar na semana corrente (segunda a domingo).
 */
class StartWorkoutSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val templates: WorkoutTemplateRepository,
    private val buildPrefilledSets: BuildPrefilledSetsUseCase,
    private val clock: Clock,
) {
    /** @return o id da sessão criada. */
    suspend operator fun invoke(templateId: Long?, makeupForDate: LocalDate? = null): SessionResult<Long> {
        if (repository.getActiveId() != null) return SessionResult.Failure(SessionError.AlreadyActive)

        val today = LocalDate.now(clock)
        if (makeupForDate != null && (templateId == null || !isLinkableDay(makeupForDate, today))) {
            return SessionResult.Failure(SessionError.InvalidMakeupDate)
        }
        val template = templateId?.let { templates.getById(it) ?: return SessionResult.Failure(SessionError.NotFound) }

        val exercises = template?.exercises.orEmpty().map {
            NewSessionExercise(
                exerciseId = it.exerciseId,
                targetReps = it.targetReps,
                sets = buildPrefilledSets(it.exerciseId, it.targetSets),
            )
        }
        val session = NewWorkoutSession(
            templateId = template?.id,
            templateName = template?.name,
            date = today,
            startedAt = clock.instant(),
            makeupForDate = makeupForDate,
            exercises = exercises,
        )
        return SessionResult.Success(repository.create(session))
    }
}
