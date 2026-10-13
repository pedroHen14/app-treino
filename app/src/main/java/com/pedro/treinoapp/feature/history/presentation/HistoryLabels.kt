package com.pedro.treinoapp.feature.history.presentation

import android.content.Context
import androidx.annotation.StringRes
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.history.domain.model.RecordType
import com.pedro.treinoapp.feature.session.presentation.formatWeight
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Os textos do app são em português; datas seguem o mesmo idioma, independente do aparelho. */
private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL 'de' yyyy", PT_BR)
private val SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, d MMM", PT_BR)
private val LONG_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT_BR)
private val DAY_OF_WEEK_FORMAT = DateTimeFormatter.ofPattern("EEE", PT_BR)
private val CHART_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM", PT_BR)

/** "Outubro de 2026". */
fun LocalDate.formatMonth(): String = format(MONTH_FORMAT).replaceFirstChar { it.titlecase(PT_BR) }

/** "Qui, 8 out". */
fun LocalDate.formatShort(): String = format(SHORT_DATE_FORMAT).removeDots().replaceFirstChar { it.titlecase(PT_BR) }

/** "Quinta-feira, 8 de outubro de 2026". */
fun LocalDate.formatLong(): String = format(LONG_DATE_FORMAT).replaceFirstChar { it.titlecase(PT_BR) }

/** "QUI", para a coluna de data dos cards. */
fun LocalDate.formatDayOfWeek(): String = format(DAY_OF_WEEK_FORMAT).removeDots().uppercase(PT_BR)

/** "08/10", eixo do gráfico. */
fun LocalDate.formatChart(): String = format(CHART_DATE_FORMAT)

private fun String.removeDots() = replace(".", "")

/** "45 min" ou "1 h 05 min". */
fun Duration.formatDuration(context: Context): String {
    val minutes = toMinutes().coerceAtLeast(0)
    return if (minutes < 60) {
        context.getString(R.string.history_duration_minutes, minutes)
    } else {
        context.getString(R.string.history_duration_hours, minutes / 60, minutes % 60)
    }
}

/** Volume (gramas × repetições) em kg, com separador de milhar: "12.450 kg" ou "1.237,5 kg". */
fun formatVolumeKg(volume: Long, context: Context): String {
    val kg = BigDecimal(volume).movePointLeft(3).stripTrailingZeros()
    val number = NumberFormat.getNumberInstance(PT_BR).apply { maximumFractionDigits = 3 }.format(kg)
    return context.getString(R.string.history_kg, number)
}

/** "62,5 × 10". */
fun formatSet(weightGrams: Int, reps: Int, context: Context): String =
    context.getString(R.string.history_set, formatWeight(weightGrams), reps)

@StringRes
fun RecordType.labelRes(): Int = when (this) {
    RecordType.MAX_WEIGHT -> R.string.record_max_weight
    RecordType.MAX_REPS_AT_WEIGHT -> R.string.record_max_reps
}
