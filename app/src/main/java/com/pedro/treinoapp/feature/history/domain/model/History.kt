package com.pedro.treinoapp.feature.history.domain.model

import com.pedro.treinoapp.feature.history.domain.Volume
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/** Sessão finalizada como sai do banco, antes de contar os recordes. */
data class FinishedSession(
    val id: Long,
    val templateName: String?,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant,
    val makeupForDate: LocalDate?,
    val exerciseCount: Int,
)

/** Item da lista do histórico. `templateName` nulo é treino avulso. */
data class SessionSummary(
    val id: Long,
    val templateName: String?,
    val date: LocalDate,
    val duration: Duration,
    val makeupForDate: LocalDate?,
    val exerciseCount: Int,
    val recordCount: Int,
)

/** Exercício que já tem séries concluídas em sessões finalizadas. */
data class ExerciseWithHistory(
    val exerciseId: Long,
    val name: String,
    val lastDate: LocalDate,
    val sessionCount: Int,
)

/** Uma execução do exercício: as séries concluídas dele numa sessão. */
data class ExerciseExecution(
    val sessionId: Long,
    val date: LocalDate,
    val sets: List<RecordedSet>,
) {
    val maxWeightGrams: Int get() = sets.maxOf { it.weightGrams }
    val volume: Long get() = Volume.ofSets(sets)
}

/** Progressão de um exercício: execuções da mais recente para a mais antiga e recordes vigentes. */
data class ExerciseProgress(
    val executions: List<ExerciseExecution>,
    val records: PersonalRecords,
    val recordsBySet: Map<Long, Set<RecordType>>,
) {
    /** Pontos do gráfico em ordem cronológica: carga máxima e volume por sessão. */
    val points: List<ProgressPoint>
        get() = executions.reversed().map { ProgressPoint(it.date, it.maxWeightGrams, it.volume) }
}

data class ProgressPoint(val date: LocalDate, val maxWeightGrams: Int, val volume: Long)
