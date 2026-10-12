package com.pedro.treinoapp.feature.weeklysummary.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeeklySummary
import com.pedro.treinoapp.feature.weeklysummary.domain.usecase.ObserveWeeklySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WeeklySummaryCardViewModel @Inject constructor(
    observeWeeklySummary: ObserveWeeklySummaryUseCase,
) : ViewModel() {

    /** Nulo enquanto carrega. */
    val summary: StateFlow<WeeklySummary?> =
        observeWeeklySummary().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
