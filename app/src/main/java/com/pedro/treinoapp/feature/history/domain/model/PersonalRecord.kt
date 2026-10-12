package com.pedro.treinoapp.feature.history.domain.model

import java.time.LocalDate

/** Série concluída de um exercício, como entra no cálculo de recordes. Carga em gramas. */
data class RecordedSet(
    val setId: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val date: LocalDate,
    val weightGrams: Int,
    val reps: Int,
)

enum class RecordType {
    /** Maior carga já registrada no exercício. */
    MAX_WEIGHT,

    /** Mais repetições já feitas com a mesma carga. */
    MAX_REPS_AT_WEIGHT,
}

/** Melhor marca vigente de uma carga: quantas repetições e quando foram feitas. */
data class RecordMark(val weightGrams: Int, val reps: Int, val date: LocalDate)

/**
 * Recordes vigentes de um exercício. [maxWeight] é a primeira série com a maior carga;
 * [bestRepsByWeight] traz a melhor marca de cada carga, da maior para a menor.
 */
data class PersonalRecords(
    val maxWeight: RecordMark?,
    val bestRepsByWeight: List<RecordMark>,
)

/** Resultado do cálculo: os recordes batidos por série (só séries com recorde aparecem) e os vigentes. */
data class RecordAnalysis(
    val recordsBySet: Map<Long, Set<RecordType>>,
    val current: PersonalRecords,
)
