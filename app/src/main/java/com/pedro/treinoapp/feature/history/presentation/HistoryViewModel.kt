package com.pedro.treinoapp.feature.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.history.domain.model.SessionSummary
import com.pedro.treinoapp.feature.history.domain.usecase.ObserveExercisesWithHistoryUseCase
import com.pedro.treinoapp.feature.history.domain.usecase.ObserveSessionHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

/** Linhas da aba de sessões (agrupadas por mês) e da aba de exercícios. */
sealed interface HistoryListItem {
    val key: String

    data class Month(val month: YearMonth) : HistoryListItem {
        override val key get() = "m$month"
    }

    data class Session(val summary: SessionSummary) : HistoryListItem {
        override val key get() = "s${summary.id}"
    }

    data class Exercise(val exercise: ExerciseWithHistory) : HistoryListItem {
        override val key get() = "e${exercise.exerciseId}"
    }
}

enum class HistoryTab { SESSIONS, EXERCISES }

data class HistoryUiState(
    val isLoading: Boolean = true,
    val tab: HistoryTab = HistoryTab.SESSIONS,
    val items: List<HistoryListItem> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeSessions: ObserveSessionHistoryUseCase,
    observeExercises: ObserveExercisesWithHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        savedStateHandle.getStateFlow(KEY_TAB, HistoryTab.SESSIONS),
        observeSessions(),
        observeExercises(),
    ) { tab, sessions, exercises ->
        HistoryUiState(
            isLoading = false,
            tab = tab,
            items = when (tab) {
                HistoryTab.SESSIONS -> sessions.groupedByMonth()
                HistoryTab.EXERCISES -> exercises.map { HistoryListItem.Exercise(it) }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun selectTab(tab: HistoryTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    /** As sessões já vêm da mais recente para a mais antiga; cada mês ganha um cabeçalho. */
    private fun List<SessionSummary>.groupedByMonth(): List<HistoryListItem> =
        groupBy { YearMonth.from(it.date) }.flatMap { (month, sessions) ->
            listOf(HistoryListItem.Month(month)) + sessions.map { HistoryListItem.Session(it) }
        }

    private companion object {
        const val KEY_TAB = "tab"
    }
}
