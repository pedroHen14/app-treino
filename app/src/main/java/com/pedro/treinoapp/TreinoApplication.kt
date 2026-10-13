package com.pedro.treinoapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.pedro.treinoapp.core.di.ApplicationScope
import com.pedro.treinoapp.feature.settings.domain.usecase.ObserveThemeModeUseCase
import com.pedro.treinoapp.feature.settings.presentation.toNightMode
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class TreinoApplication : Application() {

    @Inject
    lateinit var observeThemeMode: ObserveThemeModeUseCase

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Lê o tema antes da primeira Activity para não abrir em um tema e trocar em seguida.
        val initialMode = runBlocking { observeThemeMode().first() }
        AppCompatDelegate.setDefaultNightMode(initialMode.toNightMode())
        applicationScope.launch {
            observeThemeMode().collect { AppCompatDelegate.setDefaultNightMode(it.toNightMode()) }
        }
    }
}
