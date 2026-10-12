package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeTest {

    @Test
    fun `set volume is weight times reps`() {
        assertEquals(600_000L, Volume.ofSet(weightGrams = 60_000, reps = 10))
    }

    @Test
    fun `exercise volume is the sum of its sets`() {
        val sets = listOf(recordedSet(1, weightGrams = 60_000, reps = 10), recordedSet(2, weightGrams = 62_500, reps = 8))

        assertEquals(1_100_000L, Volume.ofSets(sets))
    }

    @Test
    fun `volume does not overflow at the maximum values`() {
        val sets = List(WorkoutSet.MAX_SETS) {
            recordedSet(it.toLong(), weightGrams = WorkoutSet.MAX_WEIGHT_GRAMS, reps = WorkoutSet.MAX_REPS)
        }

        assertEquals(19_980_000_000L, Volume.ofSets(sets))
    }
}
