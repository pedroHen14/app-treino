package com.pedro.treinoapp.feature.weeklysummary.domain

import com.pedro.treinoapp.core.time.weekStart
import com.pedro.treinoapp.feature.history.domain.PersonalRecordCalculator
import com.pedro.treinoapp.feature.history.domain.Volume
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeekDay
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeeklySummary
import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Monta o resumo da semana de [today] (segunda a domingo) a partir da agenda atual, das sessões
 * finalizadas e das séries concluídas de todo o histórico.
 *
 * Cada sessão cumpre no máximo um dia, com a mesma regra da reposição: o dia está feito se nele houve
 * sessão do modelo agendado que não era reposição de outro dia, ou reposto se alguma sessão o repôs.
 * As sessões que não cumprem nenhum dia são extras: treino avulso sem vínculo de reposição, outro
 * modelo no dia, uma segunda sessão no mesmo dia ou reposição de um dia que deixou de estar agendado.
 *
 * Recordes contam séries da semana que superaram tudo o que veio antes no histórico do exercício.
 */
object WeeklySummaryCalculator {

    fun calculate(
        today: LocalDate,
        schedule: List<ScheduleDay>,
        sessions: List<SessionDay>,
        sets: List<RecordedSet>,
    ): WeeklySummary {
        val weekStart = today.weekStart()
        val week = weekStart..weekStart.plusDays(6)
        val unmatched = sessions.filter { it.date in week }.toMutableList()

        val days = DayOfWeek.entries.map { dayOfWeek ->
            val date = weekStart.plusDays(dayOfWeek.value - 1L)
            val scheduled = schedule.find { it.dayOfWeek == dayOfWeek }
            val templateId = scheduled?.templateId
            val status = when {
                templateId == null -> DayStatus.REST
                unmatched.consume { it.date == date && it.makeupForDate == null && it.templateId == templateId } ->
                    DayStatus.DONE
                unmatched.consume { it.makeupForDate == date } -> DayStatus.MADE_UP
                date < today -> DayStatus.MISSED
                else -> DayStatus.PENDING
            }
            WeekDay(date, scheduled?.templateName.takeIf { templateId != null }, status)
        }

        val weekSets = sets.filter { it.date in week }
        val weekSetIds = weekSets.map { it.setId }.toSet()
        val recordCount = sets.groupBy { it.exerciseId }.values
            .flatMap { PersonalRecordCalculator.analyze(it).recordsBySet.keys }
            .count { it in weekSetIds }

        return WeeklySummary(
            today = today,
            days = days,
            extraCount = unmatched.size,
            volume = Volume.ofSets(weekSets),
            recordCount = recordCount,
        )
    }

    /** Remove a primeira sessão que atende [predicate]; diz se encontrou. */
    private fun MutableList<SessionDay>.consume(predicate: (SessionDay) -> Boolean): Boolean {
        val index = indexOfFirst(predicate)
        if (index < 0) return false
        removeAt(index)
        return true
    }
}
