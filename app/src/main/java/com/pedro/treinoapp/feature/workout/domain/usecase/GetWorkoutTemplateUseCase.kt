package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

class GetWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
) {
    suspend operator fun invoke(id: Long): WorkoutTemplate? = repository.getById(id)
}
