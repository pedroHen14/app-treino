package com.pedro.treinoapp.feature.exercise.data.remote

import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WgerExerciseMapperTest {

    private val english = TranslationDto(name = "Benchpress Dumbbells", language = 2, description = "<p>Lie on the bench.</p>")
    private val portuguese = TranslationDto(name = " Supino com halteres ", language = 7, description = "<p>Deite no banco.</p>")
    private val german = TranslationDto(name = "Bankdrücken Kurzhantel", language = 1)

    private fun exercise(
        translations: List<TranslationDto>,
        muscles: List<Int> = listOf(4),
        category: Int? = 11,
    ) = ExerciseInfoDto(
        id = 75,
        category = category?.let { CategoryDto(it) },
        muscles = muscles.map { MuscleDto(it) },
        translations = translations,
    )

    @Test
    fun `prefers the portuguese translation`() {
        val result = exercise(listOf(german, english, portuguese)).toDomain()!!

        assertEquals(75L, result.wgerId)
        assertEquals("Supino com halteres", result.name)
        assertTrue(result.isNameInPortuguese)
        assertEquals("Deite no banco.", result.description)
    }

    @Test
    fun `falls back to english name when there is no portuguese translation`() {
        val result = exercise(listOf(german, english)).toDomain()!!

        assertEquals("Benchpress Dumbbells", result.name)
        assertFalse(result.isNameInPortuguese)
        assertEquals("Lie on the bench.", result.description)
    }

    @Test
    fun `uses english description when the portuguese one is empty`() {
        val result = exercise(listOf(english, portuguese.copy(description = "<p></p>"))).toDomain()!!

        assertEquals("Supino com halteres", result.name)
        assertEquals("Lie on the bench.", result.description)
    }

    @Test
    fun `ignores blank portuguese name`() {
        val result = exercise(listOf(portuguese.copy(name = " "), english)).toDomain()!!

        assertEquals("Benchpress Dumbbells", result.name)
    }

    @Test
    fun `skips exercise without portuguese or english translation`() {
        assertNull(exercise(listOf(german)).toDomain())
    }

    @Test
    fun `muscle group comes from the first mapped primary muscle`() {
        assertEquals(MuscleGroup.HAMSTRINGS, exercise(listOf(english), muscles = listOf(3, 11, 8)).toDomain()!!.muscleGroup)
    }

    @Test
    fun `muscle group falls back to category`() {
        assertEquals(MuscleGroup.BACK, exercise(listOf(english), muscles = emptyList(), category = 12).toDomain()!!.muscleGroup)
    }

    @Test
    fun `muscle group is null when neither muscles nor category map`() {
        val cardio = exercise(listOf(english), muscles = listOf(3), category = 15).toDomain()!!

        assertNull(cardio.muscleGroup)
    }

    @Test
    fun `converts html description to plain text`() {
        val html = "<p>Fique em pé&nbsp;com os pés afastados.</p>\n<ul><li>Desça devagar</li><li>Suba &amp; repita</li></ul>"

        assertEquals("Fique em pé com os pés afastados.\n• Desça devagar\n• Suba & repita", html.htmlToPlainText())
    }

    @Test
    fun `raw line breaks inside a paragraph become spaces`() {
        val html = "<p>Adjust the weights based on the\nmotion.</p>\n<p>Repeat.</p>"

        assertEquals("Adjust the weights based on the motion.\nRepeat.", html.htmlToPlainText())
    }

    @Test
    fun `empty html description becomes null`() {
        assertNull("<p> </p>\n".htmlToPlainText())
    }
}
