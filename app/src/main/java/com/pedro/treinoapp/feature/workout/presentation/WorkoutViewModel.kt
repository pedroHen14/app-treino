package com.pedro.treinoapp.feature.workout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.domain.usecase.DeleteWorkoutTemplateUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.DuplicateWorkoutTemplateUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.GetTemplateScheduledDaysUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWeeklyScheduleUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWorkoutTemplatesUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.SetScheduleDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class WorkoutUiState(
    val schedule: List<ScheduleDay> = emptyList(),
    val templates: List<WorkoutTemplateSummary> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface WorkoutEvent {
    data class ShowError(val error: WorkoutError) : WorkoutEvent
    data class Duplicated(val templateId: Long) : WorkoutEvent
    data object Deleted : WorkoutEvent

    /** Pede confirmação; [scheduledDays] são os dias que vão virar descanso. */
    data class ConfirmDelete(
        val template: WorkoutTemplateSummary,
        val scheduledDays: List<DayOfWeek>,
    ) : WorkoutEvent
}

/** Formatos (dos resources) do nome da cópia: "%1$s (cópia)" e "%1$s (cópia %2$d)". */
data class CopyNameFormats(val first: String, val numbered: String) {
    fun format(original: String, copyNumber: Int): String =
        if (copyNumber == 1) first.format(original) else numbered.format(original, copyNumber)
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    observeSchedule: ObserveWeeklyScheduleUseCase,
    observeTemplates: ObserveWorkoutTemplatesUseCase,
    private val setScheduleDay: SetScheduleDayUseCase,
    private val duplicateTemplate: DuplicateWorkoutTemplateUseCase,
    private val getScheduledDays: GetTemplateScheduledDaysUseCase,
    private val deleteTemplate: DeleteWorkoutTemplateUseCase,
) : ViewModel() {

    val uiState: StateFlow<WorkoutUiState> = combine(observeSchedule(), observeTemplates()) { schedule, templates ->
        WorkoutUiState(schedule = schedule, templates = templates, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    private val _events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEvent> = _events.receiveAsFlow()

    fun setDay(dayOfWeek: DayOfWeek, templateId: Long?) = viewModelScope.launch {
        val result = setScheduleDay(dayOfWeek, templateId)
        if (result is WorkoutResult.Failure) _events.send(WorkoutEvent.ShowError(result.error))
    }

    fun duplicate(template: WorkoutTemplateSummary, formats: CopyNameFormats) = viewModelScope.launch {
        when (val result = duplicateTemplate(template.id, formats::format)) {
            is WorkoutResult.Success -> _events.send(WorkoutEvent.Duplicated(result.value))
            is WorkoutResult.Failure -> _events.send(WorkoutEvent.ShowError(result.error))
        }
    }

    fun requestDelete(template: WorkoutTemplateSummary) = viewModelScope.launch {
        _events.send(WorkoutEvent.ConfirmDelete(template, getScheduledDays(template.id)))
    }

    fun delete(template: WorkoutTemplateSummary) = viewModelScope.launch {
        when (val result = deleteTemplate(template.id)) {
            is WorkoutResult.Success -> _events.send(WorkoutEvent.Deleted)
            is WorkoutResult.Failure -> _events.send(WorkoutEvent.ShowError(result.error))
        }
    }
}
