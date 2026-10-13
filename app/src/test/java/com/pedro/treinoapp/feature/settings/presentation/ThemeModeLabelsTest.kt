package com.pedro.treinoapp.feature.settings.presentation

import androidx.appcompat.app.AppCompatDelegate
import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeLabelsTest {

    @Test
    fun `Ferro é o modo escuro, Giz o claro e Sistema segue o aparelho`() {
        assertEquals(AppCompatDelegate.MODE_NIGHT_YES, ThemeMode.FERRO.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_NO, ThemeMode.GIZ.toNightMode())
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, ThemeMode.SYSTEM.toNightMode())
    }
}
