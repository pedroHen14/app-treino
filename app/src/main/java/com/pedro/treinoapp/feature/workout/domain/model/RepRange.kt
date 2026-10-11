package com.pedro.treinoapp.feature.workout.domain.model

/** Repetições-alvo: valor fixo (`min == max`, ex.: 10) ou faixa (ex.: 8–12). */
data class RepRange(val min: Int, val max: Int) {

    val isFixed: Boolean get() = min == max

    companion object {
        const val MIN_REPS = 1
        const val MAX_REPS = 100

        fun fixed(reps: Int) = RepRange(reps, reps)
    }
}
