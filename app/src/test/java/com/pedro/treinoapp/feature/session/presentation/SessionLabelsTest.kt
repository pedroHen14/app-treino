package com.pedro.treinoapp.feature.session.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLabelsTest {

    @Test
    fun `carga inteira mostra uma casa decimal`() {
        assertEquals("80,0", formatWeight(80_000))
        assertEquals("0,0", formatWeight(0))
    }

    @Test
    fun `carga fracionada usa vírgula`() {
        assertEquals("82,5", formatWeight(82_500))
        assertEquals("2,5", formatWeight(2_500))
    }

    @Test
    fun `fração menor que meio quilo não é arredondada`() {
        assertEquals("1,25", formatWeight(1_250))
    }

    @Test
    fun `carga formatada volta ao mesmo valor em gramas`() {
        listOf(0, 500, 80_000, 82_500, 1_250).forEach {
            assertEquals(it, parseWeight(formatWeight(it)).getOrNull())
        }
    }
}
