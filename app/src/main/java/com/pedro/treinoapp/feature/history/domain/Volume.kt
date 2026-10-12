package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.history.domain.model.RecordedSet

/**
 * Volume em gramas × repetições. Usa `Long`: 1.000 kg × 999 reps × 20 séries não cabe em `Int`.
 */
object Volume {

    /** Volume de uma série = carga × repetições. */
    fun ofSet(weightGrams: Int, reps: Int): Long = weightGrams.toLong() * reps

    /** Volume do exercício na sessão = soma das séries. */
    fun ofSets(sets: List<RecordedSet>): Long = sets.sumOf { ofSet(it.weightGrams, it.reps) }
}
