package com.pedro.treinoapp.feature.weeklysummary.domain.model

import java.time.LocalDate

enum class DayStatus {
    /** Dia agendado com sessão do próprio modelo nele. */
    DONE,

    /** Dia agendado que não foi feito, mas teve uma sessão de reposição. */
    MADE_UP,

    /** Dia agendado cumprido antes, por uma sessão de um dia anterior que o adiantou. */
    ADVANCED,

    /** Dia agendado, hoje ou adiante, ainda não cumprido. */
    PENDING,

    /** Dia agendado já passado, nem feito nem reposto. */
    MISSED,

    /** Dia sem modelo na agenda. */
    REST,

    /** Dia sem modelo na agenda em que houve sessão que repôs ou adiantou outro dia. */
    TRAINED,
}

/**
 * Um dia da semana corrente. `templateName` é o modelo agendado; nulo no descanso. Em [DayStatus.TRAINED],
 * é o modelo do dia que a sessão cumpriu.
 */
data class WeekDay(
    val date: LocalDate,
    val templateName: String?,
    val status: DayStatus,
)

/**
 * Resumo da semana de [today]: os sete dias de segunda a domingo, sessões que não cumprem nenhum dia
 * agendado, volume (gramas × repetições) e quantas séries bateram recorde.
 */
data class WeeklySummary(
    val today: LocalDate,
    val days: List<WeekDay>,
    val extraCount: Int,
    val volume: Long,
    val recordCount: Int,
) {
    val plannedCount: Int get() = days.count { it.status != DayStatus.REST && it.status != DayStatus.TRAINED }

    /** Reposto e adiantado contam como cumpridos. */
    val doneCount: Int get() = days.count { it.status in DONE_STATUSES }

    private companion object {
        val DONE_STATUSES = setOf(DayStatus.DONE, DayStatus.MADE_UP, DayStatus.ADVANCED)
    }
}
