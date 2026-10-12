package com.pedro.treinoapp.feature.workout.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateExerciseTest {

    private fun fromInput(sets: String, repsMin: String, repsMax: String) =
        TemplateExercise.fromInput(1, "Supino", sets, repsMin, repsMax)

    @Test
    fun `empty sets and min reps use the suggestion as fixed reps`() {
        val exercise = fromInput(sets = "", repsMin = "", repsMax = "")

        assertEquals(TemplateExercise.DEFAULT_SETS, exercise.targetSets)
        assertEquals(RepRange.fixed(TemplateExercise.DEFAULT_REPS.min), exercise.targetReps)
    }

    @Test
    fun `typed values are kept`() {
        val exercise = fromInput(sets = "4", repsMin = "6", repsMax = "10")

        assertEquals(4, exercise.targetSets)
        assertEquals(RepRange(6, 10), exercise.targetReps)
    }

    @Test
    fun `empty max reps means fixed reps`() {
        assertEquals(RepRange.fixed(10), fromInput(sets = "3", repsMin = "10", repsMax = "").targetReps)
    }

    @Test
    fun `empty min reps uses the suggestion with the typed max`() {
        assertEquals(RepRange(TemplateExercise.DEFAULT_REPS.min, 15), fromInput("", "", "15").targetReps)
    }

    @Test
    fun `spaces are trimmed and only spaces count as empty`() {
        val exercise = fromInput(sets = " 5 ", repsMin = "  ", repsMax = " 12")

        assertEquals(5, exercise.targetSets)
        assertEquals(RepRange(TemplateExercise.DEFAULT_REPS.min, 12), exercise.targetReps)
    }

    @Test
    fun `invalid text becomes zero for validation to reject`() {
        val exercise = fromInput(sets = "x", repsMin = "-", repsMax = "a")

        assertEquals(0, exercise.targetSets)
        assertEquals(RepRange(0, 0), exercise.targetReps)
    }
}
