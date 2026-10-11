package com.pedro.treinoapp.feature.session.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.TodayPlan
import com.pedro.treinoapp.feature.session.domain.usecase.GetActiveSessionIdUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.ObserveTodayPlanUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.StartWorkoutSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TodayWorkoutCardEvent {
    data class OpenSession(val sessionId: Long) : TodayWorkoutCardEvent
    data class ShowError(val error: SessionError) : TodayWorkoutCardEvent
}

@HiltViewModel
class TodayWorkoutCardViewModel @Inject constructor(
    observeTodayPlan: ObserveTodayPlanUseCase,
    private val startSession: StartWorkoutSessionUseCase,
    private val getActiveSessionId: GetActiveSessionIdUseCase,
) : ViewModel() {

    /** Nulo enquanto carrega. */
    val plan: StateFlow<TodayPlan?> =
        observeTodayPlan().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<TodayWorkoutCardEvent>(Channel.BUFFERED)
    val events: Flow<TodayWorkoutCardEvent> = _events.receiveAsFlow()

    private var isStarting = false

    /** O modelo agendado para hoje nunca é reposição, então inicia direto. */
    fun startScheduled() {
        val templateId = plan.value?.scheduledTemplateId ?: return
        if (isStarting) return
        isStarting = true
        viewModelScope.launch {
            when (val result = startSession(templateId)) {
                is SessionResult.Success -> _events.send(TodayWorkoutCardEvent.OpenSession(result.value))
                is SessionResult.Failure -> openActiveOrShow(result.error)
            }
            isStarting = false
        }
    }

    fun resume() {
        val sessionId = plan.value?.activeSessionId ?: return
        viewModelScope.launch { _events.send(TodayWorkoutCardEvent.OpenSession(sessionId)) }
    }

    private suspend fun openActiveOrShow(error: SessionError) {
        val activeId = if (error == SessionError.AlreadyActive) getActiveSessionId() else null
        _events.send(
            if (activeId != null) TodayWorkoutCardEvent.OpenSession(activeId) else TodayWorkoutCardEvent.ShowError(error),
        )
    }
}
