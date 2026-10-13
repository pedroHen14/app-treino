package com.pedro.treinoapp.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.domain.usecase.ObserveThemeModeUseCase
import com.pedro.treinoapp.feature.settings.domain.usecase.SetThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Escolha de tema (Ferro, Giz ou sistema). A aplicação do modo fica com o TreinoApplication. */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModel() {

    // Eagerly: a tela só lê o valor ao abrir o diálogo, sem coletar o fluxo.
    val themeMode: StateFlow<ThemeMode> =
        observeThemeMode().stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DEFAULT)

    fun select(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }
}
