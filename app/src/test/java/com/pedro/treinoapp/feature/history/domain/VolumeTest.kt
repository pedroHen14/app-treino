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
        assertEquals(1_100_000L, Volume.ofSets(listOf(60_000 to 10, 62_500 to 8)))
    }

    @Test
    fun `volume does not overflow at the maximum values`() {
        val sets = List(WorkoutSet.MAX_SETS) { WorkoutSet.MAX_WEIGHT_GRAMS to WorkoutSet.MAX_REPS }

        assertEquals(19_980_000_000L, Volume.ofSets(sets))
    }
}
