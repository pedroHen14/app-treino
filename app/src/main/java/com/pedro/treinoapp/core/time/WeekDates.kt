package com.pedro.treinoapp.core.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A semana do app vai de segunda a domingo. */
fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun LocalDate.weekEnd(): LocalDate = with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
