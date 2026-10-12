package com.pedro.treinoapp.feature.history.domain

/**
 * Volume em gramas × repetições. Usa `Long`: 1.000 kg × 999 reps × 20 séries não cabe em `Int`.
 */
object Volume {

    /** Volume de uma série = carga × repetições. */
    fun ofSet(weightGrams: Int, reps: Int): Long = weightGrams.toLong() * reps

    /** Volume do exercício na sessão = soma das séries, dadas como pares (carga em gramas, repetições). */
    fun ofSets(sets: List<Pair<Int, Int>>): Long = sets.sumOf { (weightGrams, reps) -> ofSet(weightGrams, reps) }
}
