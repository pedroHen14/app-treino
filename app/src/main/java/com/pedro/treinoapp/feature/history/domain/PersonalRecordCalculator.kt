package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.history.domain.model.PersonalRecords
import com.pedro.treinoapp.feature.history.domain.model.RecordAnalysis
import com.pedro.treinoapp.feature.history.domain.model.RecordMark
import com.pedro.treinoapp.feature.history.domain.model.RecordType
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet

/**
 * Avalia recordes pessoais de **um** exercício. As séries chegam em ordem cronológica e cada uma é
 * comparada com todas as anteriores, inclusive da mesma sessão. Só é recorde o que supera uma marca
 * já registrada: a primeira execução (e a primeira vez com uma carga) vira referência, e empate não conta.
 */
object PersonalRecordCalculator {

    fun analyze(sets: List<RecordedSet>): RecordAnalysis {
        val recordsBySet = mutableMapOf<Long, Set<RecordType>>()
        var maxWeight: RecordMark? = null
        val bestByWeight = mutableMapOf<Int, RecordMark>()

        sets.forEach { set ->
            val records = buildSet {
                maxWeight?.let { if (set.weightGrams > it.weightGrams) add(RecordType.MAX_WEIGHT) }
                bestByWeight[set.weightGrams]?.let { if (set.reps > it.reps) add(RecordType.MAX_REPS_AT_WEIGHT) }
            }
            if (records.isNotEmpty()) recordsBySet[set.setId] = records

            val mark = RecordMark(set.weightGrams, set.reps, set.date)
            if (maxWeight.let { it == null || set.weightGrams > it.weightGrams }) maxWeight = mark
            if (bestByWeight[set.weightGrams].let { it == null || set.reps > it.reps }) bestByWeight[set.weightGrams] = mark
        }

        return RecordAnalysis(
            recordsBySet = recordsBySet,
            current = PersonalRecords(
                maxWeight = maxWeight,
                bestRepsByWeight = bestByWeight.values.sortedByDescending { it.weightGrams },
            ),
        )
    }
}
