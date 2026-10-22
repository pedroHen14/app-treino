package com.pedro.treinoapp.feature.history.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.core.time.weekStart
import com.pedro.treinoapp.feature.history.domain.Volume
import com.pedro.treinoapp.feature.history.domain.model.RecordType
import com.pedro.treinoapp.feature.history.domain.usecase.ObserveSessionRecordsUseCase
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.isLinkableDay
import com.pedro.treinoapp.feature.session.domain.usecase.DeleteFinishedSessionUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.LinkSessionToDayUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.ObserveSessionUseCase
import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWeeklyScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

data class DetailSet(val number: Int, val weightGrams: Int, val reps: Int, val records: Set<RecordType>)

data class DetailExercise(
    val exerciseId: Long,
    val name: String,
    val sets: List<DetailSet>,
    val volume: Long,
)

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    /** Sessão excluída, inexistente ou ainda em andamento: a tela fecha. */
    val isClosed: Boolean = false,
    val templateName: String? = null,
    val date: LocalDate? = null,
    val duration: Duration = Duration.ZERO,
    val makeupForDate: LocalDate? = null,
    val exercises: List<DetailExercise> = emptyList(),
    /** Só sessões de um modelo podem ser vinculadas a um dia da agenda. */
    val canLink: Boolean = false,
    /** Dias agendados da semana da sessão, exceto o dela, que ela pode repor ou adiantar. */
    val linkOptions: List<LinkOption> = emptyList(),
)

data class LinkOption(val date: LocalDate, val templateName: String)

sealed interface SessionDetailEvent {
    data class ShowError(val error: SessionError) : SessionDetailEvent
    data object Deleted : SessionDetailEvent
    data object Linked : SessionDetailEvent
}

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeSession: ObserveSessionUseCase,
    observeRecords: ObserveSessionRecordsUseCase,
    observeSchedule: ObserveWeeklyScheduleUseCase,
    private val deleteSession: DeleteFinishedSessionUseCase,
    private val linkSession: LinkSessionToDayUseCase,
) : ViewModel() {

    val sessionId: Long = checkNotNull(savedStateHandle[SessionDetailFragment.ARG_SESSION_ID])

    private val _events = Channel<SessionDetailEvent>(Channel.BUFFERED)
    val events: Flow<SessionDetailEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SessionDetailUiState> =
        combine(observeSession(sessionId), observeRecords(sessionId), observeSchedule()) { session, records, schedule ->
            val finishedAt = session?.finishedAt
            if (session == null || finishedAt == null) {
                SessionDetailUiState(isLoading = false, isClosed = true)
            } else {
                SessionDetailUiState(
                    isLoading = false,
                    templateName = session.templateName,
                    date = session.date,
                    duration = Duration.between(session.startedAt, finishedAt),
                    makeupForDate = session.makeupForDate,
                    exercises = session.toDetailExercises(records),
                    canLink = session.templateId != null,
                    linkOptions = session.linkOptions(schedule),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    /** [date] nulo deixa a sessão sem vínculo. */
    fun link(date: LocalDate?) = viewModelScope.launch {
        when (val result = linkSession(sessionId, date)) {
            is SessionResult.Success -> _events.send(SessionDetailEvent.Linked)
            is SessionResult.Failure -> _events.send(SessionDetailEvent.ShowError(result.error))
        }
    }

    private fun WorkoutSession.linkOptions(schedule: List<ScheduleDay>): List<LinkOption> {
        val weekStart = date.weekStart()
        return schedule.mapNotNull { day ->
            val name = day.templateName ?: return@mapNotNull null
            LinkOption(weekStart.plusDays(day.dayOfWeek.value - 1L), name).takeIf { isLinkableDay(it.date, date) }
        }
    }

    fun delete() = viewModelScope.launch {
        when (val result = deleteSession(sessionId)) {
            is SessionResult.Success -> _events.send(SessionDetailEvent.Deleted)
            is SessionResult.Failure -> _events.send(SessionDetailEvent.ShowError(result.error))
        }
    }

    /** No histórico só há séries concluídas, que sempre têm carga e repetições. */
    private fun WorkoutSession.toDetailExercises(records: Map<Long, Set<RecordType>>) = exercises.map { exercise ->
        val sets = exercise.sets
            .filter { it.isCompleted }
            .mapNotNull { set ->
                val weight = set.weightGrams ?: return@mapNotNull null
                val reps = set.reps ?: return@mapNotNull null
                Triple(set.id, weight, reps)
            }
        DetailExercise(
            exerciseId = exercise.exerciseId,
            name = exercise.exerciseName,
            sets = sets.mapIndexed { index, (setId, weight, reps) ->
                DetailSet(index + 1, weight, reps, records[setId].orEmpty())
            },
            volume = sets.sumOf { (_, weight, reps) -> Volume.ofSet(weight, reps) },
        )
    }
}
