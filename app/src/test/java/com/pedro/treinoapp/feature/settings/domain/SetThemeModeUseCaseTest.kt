package com.pedro.treinoapp.feature.settings.domain

import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.domain.usecase.ObserveThemeModeUseCase
import com.pedro.treinoapp.feature.settings.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetThemeModeUseCaseTest {

    private val repository = FakeThemeSettingsRepository()
    private val observe = ObserveThemeModeUseCase(repository)
    private val set = SetThemeModeUseCase(repository)

    @Test
    fun `começa em Ferro`() = runTest {
        assertEquals(ThemeMode.FERRO, observe().first())
    }

    @Test
    fun `tema escolhido passa a ser observado`() = runTest {
        set(ThemeMode.GIZ)
        assertEquals(ThemeMode.GIZ, observe().first())

        set(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, observe().first())
    }
}
