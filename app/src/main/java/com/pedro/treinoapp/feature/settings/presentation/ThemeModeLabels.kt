package com.pedro.treinoapp.feature.settings.presentation

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode

/** Modo noturno do AppCompat: Ferro é o tema escuro, Giz o claro. */
fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.FERRO -> AppCompatDelegate.MODE_NIGHT_YES
    ThemeMode.GIZ -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}

@get:StringRes
val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.FERRO -> R.string.theme_ferro
        ThemeMode.GIZ -> R.string.theme_giz
        ThemeMode.SYSTEM -> R.string.theme_system
    }
