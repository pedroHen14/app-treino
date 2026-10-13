package com.pedro.treinoapp.feature.settings.domain

import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `padrão é Ferro`() {
        assertEquals(ThemeMode.FERRO, ThemeMode.DEFAULT)
    }

    @Test
    fun `sem valor salvo usa o padrão`() {
        assertEquals(ThemeMode.FERRO, ThemeMode.fromStorage(null))
    }

    @Test
    fun `valor desconhecido usa o padrão`() {
        assertEquals(ThemeMode.FERRO, ThemeMode.fromStorage("ESCURO"))
    }

    @Test
    fun `valor salvo é restaurado`() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorage(it.name)) }
    }
}
