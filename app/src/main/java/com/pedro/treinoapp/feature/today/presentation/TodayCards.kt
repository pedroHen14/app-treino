package com.pedro.treinoapp.feature.today.presentation

import androidx.fragment.app.Fragment
import com.pedro.treinoapp.feature.session.presentation.today.TodayWorkoutCardFragment
import com.pedro.treinoapp.feature.weeklysummary.presentation.WeeklySummaryCardFragment

/**
 * Cards exibidos na tela Hoje, na ordem em que aparecem. Cada card é um Fragment
 * independente, mantido pela própria feature; para incluir um novo (ex.: dieta),
 * basta adicionar uma entrada aqui.
 */
data class TodayCard(val tag: String, val create: () -> Fragment)

val todayCards: List<TodayCard> = listOf(
    TodayCard("today_workout") { TodayWorkoutCardFragment() },
    TodayCard("weekly_summary") { WeeklySummaryCardFragment() },
)
