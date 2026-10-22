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
 * sessão do modelo agendado sem vínculo com outro dia; senão, reposto ou adiantado se uma sessão de
 * depois ou de antes o cumpriu. O descanso em que houve essa sessão aparece como treinado.
 * As sessões que não cumprem nenhum dia são extras: treino avulso sem vínculo, outro modelo no dia,
 * uma segunda sessão no mesmo dia ou vínculo com um dia que deixou de estar agendado.
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
        val linked = mutableListOf<SessionDay>()

        val scheduledDays = DayOfWeek.entries.map { dayOfWeek ->
            val date = weekStart.plusDays(dayOfWeek.value - 1L)
            val scheduled = schedule.find { it.dayOfWeek == dayOfWeek }
            val templateId = scheduled?.templateId
            val status = when {
                templateId == null -> DayStatus.REST
                unmatched.consume { it.date == date && it.makeupForDate == null && it.templateId == templateId } != null ->
                    DayStatus.DONE
                else -> when (val link = unmatched.consume { it.makeupForDate == date }?.also(linked::add)) {
                    null -> if (date < today) DayStatus.MISSED else DayStatus.PENDING
                    else -> if (link.date < date) DayStatus.ADVANCED else DayStatus.MADE_UP
                }
            }
            WeekDay(date, scheduled?.templateName.takeIf { templateId != null }, status)
        }

        // Dia de descanso com sessão que cumpriu outro dia conta como treinado, com o nome do modelo daquele dia.
        val days = scheduledDays.map { day ->
            val link = linked.firstOrNull { it.date == day.date }
            if (day.status != DayStatus.REST || link == null) return@map day
            val fulfilled = scheduledDays.first { it.date == link.makeupForDate }
            day.copy(templateName = fulfilled.templateName, status = DayStatus.TRAINED)
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

    /** Remove e devolve a primeira sessão que atende [predicate]. */
    private fun MutableList<SessionDay>.consume(predicate: (SessionDay) -> Boolean): SessionDay? {
        val index = indexOfFirst(predicate)
        if (index < 0) return null
        return removeAt(index)
    }
}
