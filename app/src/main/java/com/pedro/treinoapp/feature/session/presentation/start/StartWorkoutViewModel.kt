package com.pedro.treinoapp.feature.session.presentation.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.usecase.FindMakeupCandidateUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.GetActiveSessionIdUseCase
import com.pedro.treinoapp.feature.session.domain.usecase.StartWorkoutSessionUseCase
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWorkoutTemplatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface StartWorkoutEvent {
    /** O modelo estava agendado para [date], dia anterior desta semana ainda sem treino. */
    data class AskMakeup(val template: WorkoutTemplateSummary, val date: LocalDate) : StartWorkoutEvent
    data class OpenSession(val sessionId: Long) : StartWorkoutEvent
    data class ShowError(val error: SessionError) : StartWorkoutEvent
}

/** Treino avulso: qualquer modelo, ou uma sessão vazia. */
@HiltViewModel
class StartWorkoutViewModel @Inject constructor(
    observeTemplates: ObserveWorkoutTemplatesUseCase,
    private val findMakeupCandidate: FindMakeupCandidateUseCase,
    private val startSession: StartWorkoutSessionUseCase,
    private val getActiveSessionId: GetActiveSessionIdUseCase,
) : ViewModel() {

    /** Nulo enquanto carrega. */
    val templates: StateFlow<List<WorkoutTemplateSummary>?> =
        observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = Channel<StartWorkoutEvent>(Channel.BUFFERED)
    val events: Flow<StartWorkoutEvent> = _events.receiveAsFlow()

    private var isStarting = false

    /** Antes de iniciar um modelo, pergunta se é reposição quando houver dia pendente desta semana. */
    fun onTemplateChosen(template: WorkoutTemplateSummary) = launchOnce {
        val makeupDate = findMakeupCandidate(template.id)
        if (makeupDate != null) {
            _events.send(StartWorkoutEvent.AskMakeup(template, makeupDate))
        } else {
            start(template.id, makeupForDate = null)
        }
    }

    /** Resposta à pergunta de reposição: [makeupForDate] nulo quando o treino não é reposição. */
    fun onMakeupAnswered(templateId: Long, makeupForDate: LocalDate?) = launchOnce {
        start(templateId, makeupForDate)
    }

    fun onEmptySessionChosen() = launchOnce { start(templateId = null, makeupForDate = null) }

    private suspend fun start(templateId: Long?, makeupForDate: LocalDate?) {
        when (val result = startSession(templateId, makeupForDate)) {
            is SessionResult.Success -> _events.send(StartWorkoutEvent.OpenSession(result.value))
            is SessionResult.Failure -> {
                val activeId = if (result.error == SessionError.AlreadyActive) getActiveSessionId() else null
                _events.send(
                    if (activeId != null) StartWorkoutEvent.OpenSession(activeId) else StartWorkoutEvent.ShowError(result.error),
                )
            }
        }
    }

    /** Ignora toques repetidos enquanto uma escolha está sendo processada. */
    private fun launchOnce(block: suspend () -> Unit) {
        if (isStarting) return
        isStarting = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                isStarting = false
            }
        }
    }
}
